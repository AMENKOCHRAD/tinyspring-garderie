package com.tinyspring.garderie.service.boutique;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinyspring.garderie.entity.boutique.Prediction;
import com.tinyspring.garderie.repository.boutique.PredictionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests services ML et Ollama")
class MlServicesTest {

    @Mock
    private PredictionRepository predictionRepository;

    @Mock
    private RestTemplate restTemplate;

    private PredictionServiceImpl predictionService;
    private OllamaServiceImpl ollamaService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        predictionService = new PredictionServiceImpl(predictionRepository, objectMapper);
        ReflectionTestUtils.setField(predictionService, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(predictionService, "mlServiceUrl", "http://ml.test");

        ollamaService = new OllamaServiceImpl(objectMapper);
        ReflectionTestUtils.setField(ollamaService, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(ollamaService, "ollamaUrl", "http://ollama.test");
        ReflectionTestUtils.setField(ollamaService, "ollamaModel", "llama-test");
    }

    @Test
    @DisplayName("recalculerToutesLesPredictions() entraine, recupere et sauvegarde")
    void recalculerToutesLesPredictions_shouldTrainFetchAndSavePredictions() {
        Prediction oldPrediction = Prediction.builder()
                .id(1L)
                .produitId(5L)
                .build();
        Map<String, Object> produitPrediction = Map.of(
                "produit_id", 5,
                "produit_nom", "Puzzle bois",
                "stock_actuel", 3,
                "total_prevu_4_semaines", 12,
                "alerte_rupture", true,
                "semaines", List.of(Map.of("semaine", 1, "prevision", 4))
        );
        when(restTemplate.postForObject(eq("http://ml.test/train"), isNull(), eq(Map.class)))
                .thenReturn(Map.of("status", "ok"));
        when(restTemplate.getForObject(eq("http://ml.test/predict/all"), eq(Map.class)))
                .thenReturn(Map.of("produits", List.of(produitPrediction)));
        when(predictionRepository.findTopByProduitIdOrderByDateCalculDesc(5L))
                .thenReturn(Optional.of(oldPrediction));

        predictionService.recalculerToutesLesPredictions();

        ArgumentCaptor<Prediction> captor = ArgumentCaptor.forClass(Prediction.class);
        verify(predictionRepository).delete(oldPrediction);
        verify(predictionRepository).save(captor.capture());
        Prediction saved = captor.getValue();
        assertThat(saved.getProduitId()).isEqualTo(5L);
        assertThat(saved.getProduitNom()).isEqualTo("Puzzle bois");
        assertThat(saved.getStockActuel()).isEqualTo(3);
        assertThat(saved.getTotalPrevu4Semaines()).isEqualTo(12);
        assertThat(saved.getAlerteRupture()).isTrue();
        assertThat(saved.getDateCalcul()).isNotNull();
        assertThat(saved.getDetailsJson()).contains("prevision");
    }

    @Test
    @DisplayName("recalculerToutesLesPredictions() ignore une reponse null")
    void recalculerToutesLesPredictions_shouldIgnoreNullResponse() {
        when(restTemplate.postForObject(eq("http://ml.test/train"), isNull(), eq(Map.class)))
                .thenReturn(Map.of("status", "ok"));
        when(restTemplate.getForObject(eq("http://ml.test/predict/all"), eq(Map.class)))
                .thenReturn(null);

        predictionService.recalculerToutesLesPredictions();

        verify(predictionRepository, never()).save(any(Prediction.class));
    }

    @Test
    @DisplayName("recalculerToutesLesPredictions() ignore les erreurs du service ML")
    void recalculerToutesLesPredictions_shouldCatchMlErrors() {
        when(restTemplate.postForObject(eq("http://ml.test/train"), isNull(), eq(Map.class)))
                .thenThrow(new RuntimeException("ml down"));

        predictionService.recalculerToutesLesPredictions();

        verify(predictionRepository, never()).save(any(Prediction.class));
    }

    @Test
    @DisplayName("getAllPredictions() delegue au repository")
    void getAllPredictions_shouldReturnRepositoryData() {
        List<Prediction> predictions = List.of(Prediction.builder()
                .produitId(5L)
                .alerteRupture(true)
                .build());
        when(predictionRepository.findAllOrderByAlerteRuptureDescTotalPrevuDesc())
                .thenReturn(predictions);

        List<Prediction> result = predictionService.getAllPredictions();

        assertThat(result).isEqualTo(predictions);
    }

    @Test
    @DisplayName("getAlertes() delegue au repository")
    void getAlertes_shouldReturnRepositoryData() {
        List<Prediction> alertes = List.of(Prediction.builder()
                .produitId(5L)
                .alerteRupture(true)
                .build());
        when(predictionRepository.findByAlerteRuptureTrue()).thenReturn(alertes);

        List<Prediction> result = predictionService.getAlertes();

        assertThat(result).isEqualTo(alertes);
    }

    @Test
    @DisplayName("getPredictionLive() appelle le service ML")
    void getPredictionLive_shouldCallMlService() {
        Map<String, Object> response = Map.of("produit_id", 5, "score", 12);
        when(restTemplate.getForObject(eq("http://ml.test/predict/5?weeks=4"), eq(Map.class)))
                .thenReturn(response);

        Map<String, Object> result = predictionService.getPredictionLive(5L);

        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("recalculerMaintenant() relance le recalcul planifie")
    void recalculerMaintenant_shouldCallRecalculation() {
        when(restTemplate.postForObject(eq("http://ml.test/train"), isNull(), eq(Map.class)))
                .thenReturn(Map.of("status", "ok"));
        when(restTemplate.getForObject(eq("http://ml.test/predict/all"), eq(Map.class)))
                .thenReturn(null);

        predictionService.recalculerMaintenant();

        verify(restTemplate).getForObject("http://ml.test/predict/all", Map.class);
    }

    @Test
    @DisplayName("analyserProduit() extrait le JSON retourne par Ollama")
    void analyserProduit_shouldExtractJsonFromOllamaResponse() {
        when(restTemplate.postForEntity(
                eq("http://ollama.test/api/generate"),
                any(Map.class),
                eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of(
                        "response",
                        "texte avant\n{\"diagnostic\":\"Ok\",\"score_urgence\":3}\ntexte apres"
                )));

        String result = ollamaService.analyserProduit(
                "Puzzle bois",
                "Puzzle 24 pieces",
                12.50,
                3,
                0,
                "Jeux educatifs"
        );

        assertThat(result).isEqualTo("{\"diagnostic\":\"Ok\",\"score_urgence\":3}");
    }

    @Test
    @DisplayName("analyserProduit() retourne le fallback quand Ollama ne donne pas de JSON")
    void analyserProduit_shouldReturnFallbackWhenResponseHasNoJson() {
        when(restTemplate.postForEntity(
                eq("http://ollama.test/api/generate"),
                any(Map.class),
                eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("response", "aucun json exploitable")));

        String result = ollamaService.analyserProduit(
                "Puzzle bois",
                "Puzzle 24 pieces",
                12.50,
                3,
                0,
                "Jeux educatifs"
        );

        assertThat(result)
                .contains("Produit sans ventes recentes")
                .contains("promotion_recommandee");
    }
}
