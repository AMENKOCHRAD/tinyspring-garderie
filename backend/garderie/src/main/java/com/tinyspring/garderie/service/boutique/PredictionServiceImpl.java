package com.tinyspring.garderie.service.boutique;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinyspring.garderie.entity.boutique.Prediction;
import com.tinyspring.garderie.repository.boutique.PredictionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class PredictionServiceImpl implements PredictionService {

    @Value("${ml.service.url:http://localhost:5001}")
    private String mlServiceUrl;

    private final PredictionRepository predictionRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public PredictionServiceImpl(PredictionRepository predictionRepository,
                                 ObjectMapper objectMapper) {
        this.predictionRepository = predictionRepository;
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
    }

    @Override
    @Scheduled(cron = "0 0 2 * * *")
    public void recalculerToutesLesPredictions() {
        System.out.println("Recalcul des prédictions ML...");
        try {
            restTemplate.postForObject(mlServiceUrl + "/train", null, Map.class);
            Map<String, Object> response = restTemplate.getForObject(
                    mlServiceUrl + "/predict/all", Map.class);
            if (response == null) return;
            List<Map<String, Object>> produits =
                    (List<Map<String, Object>>) response.get("produits");
            for (Map<String, Object> p : produits) sauvegarderPrediction(p);
            System.out.println("Prédictions mises à jour : "
                    + produits.size() + " produits");
        } catch (Exception e) {
            System.err.println("Erreur ML service : " + e.getMessage());
        }
    }

    private void sauvegarderPrediction(Map<String, Object> data) {
        try {
            Long produitId = Long.valueOf(data.get("produit_id").toString());
            predictionRepository.findTopByProduitIdOrderByDateCalculDesc(produitId)
                    .ifPresent(predictionRepository::delete);
            Prediction prediction = Prediction.builder()
                    .produitId(produitId)
                    .produitNom(data.get("produit_nom").toString())
                    .stockActuel(Integer.valueOf(data.get("stock_actuel").toString()))
                    .totalPrevu4Semaines(Integer.valueOf(
                            data.get("total_prevu_4_semaines").toString()))
                    .alerteRupture(Boolean.valueOf(data.get("alerte_rupture").toString()))
                    .dateCalcul(LocalDateTime.now())
                    .detailsJson(objectMapper.writeValueAsString(data.get("semaines")))
                    .build();
            predictionRepository.save(prediction);
        } catch (Exception e) {
            System.err.println("Erreur sauvegarde prédiction : " + e.getMessage());
        }
    }

    @Override
    public List<Prediction> getAllPredictions() {
        return predictionRepository.findAllOrderByAlerteRuptureDescTotalPrevuDesc();
    }

    @Override
    public List<Prediction> getAlertes() {
        return predictionRepository.findByAlerteRuptureTrue();
    }

    @Override
    public Map<String, Object> getPredictionLive(Long produitId) {
        return restTemplate.getForObject(
                mlServiceUrl + "/predict/" + produitId + "?weeks=4", Map.class);
    }

    @Override
    public void recalculerMaintenant() {
        recalculerToutesLesPredictions();
    }
}