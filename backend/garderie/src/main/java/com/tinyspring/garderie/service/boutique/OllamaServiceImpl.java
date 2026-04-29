package com.tinyspring.garderie.service.boutique;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;

@Service
public class OllamaServiceImpl implements OllamaService {

    @Value("${ollama.url}")
    private String ollamaUrl;

    @Value("${ollama.model}")
    private String ollamaModel;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OllamaServiceImpl(ObjectMapper objectMapper) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
    }

    @Override
    public String analyserProduit(String nom, String description,
                                  double prix, int stock,
                                  int ventes, String categorie) {
        String prompt = String.format(
                "Tu es expert marketing pour garderies et creches. " +
                        "Analyse ce produit et reponds en JSON valide compact (une seule ligne). " +
                        "Produit: %s. Prix: %.2f EUR. Stock: %d unites. Ventes ce mois: %d. Categorie: %s. " +
                        "Reponds avec exactement ce format JSON sans aucun texte avant ou apres: " +
                        "{\"diagnostic\":\"...\",\"score_urgence\":8,\"solutions\":[" +
                        "{\"type\":\"DESCRIPTION\",\"titre\":\"...\",\"impact_estime\":\"Fort\",\"automatisable\":true}," +
                        "{\"type\":\"PROMOTION\",\"titre\":\"...\",\"impact_estime\":\"Fort\",\"automatisable\":true}," +
                        "{\"type\":\"TAGS\",\"titre\":\"...\",\"impact_estime\":\"Moyen\",\"automatisable\":true}" +
                        "],\"nouvelle_description\":\"...\",\"tags_suggeres\":[\"...\",\"...\",\"...\"]," +
                        "\"promotion_recommandee\":20,\"message_email\":{\"objet\":\"...\",\"corps\":\"...\"}}",
                nom, prix, stock, ventes, categorie);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", ollamaModel);
        requestBody.put("prompt", prompt);
        requestBody.put("stream", false);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                ollamaUrl + "/api/generate", requestBody, Map.class);

        String rawResponse = response.getBody().get("response").toString().trim();
        rawResponse = rawResponse.replaceAll("[\\r\\n\\t]", " ")
                .replaceAll("\\s+", " ").trim();

        int start = rawResponse.indexOf("{");
        int end = rawResponse.lastIndexOf("}");
        if (start != -1 && end != -1 && end > start) {
            return rawResponse.substring(start, end + 1);
        }

        return """
        {"diagnostic":"Produit sans ventes recentes","score_urgence":6,
         "solutions":[
           {"type":"DESCRIPTION","titre":"Ameliorer description",
            "impact_estime":"Fort","automatisable":true},
           {"type":"PROMOTION","titre":"Promotion -20%",
            "impact_estime":"Fort","automatisable":true}
         ],
         "nouvelle_description":"Produit de qualite pour garderie.",
         "tags_suggeres":["garderie","enfant","creche"],
         "promotion_recommandee":20,
         "message_email":{"objet":"Offre speciale","corps":"Decouvrez nos offres."}}
        """;
    }
}