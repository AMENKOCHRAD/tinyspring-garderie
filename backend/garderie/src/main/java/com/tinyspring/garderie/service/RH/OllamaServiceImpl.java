package com.tinyspring.garderie.service.RH;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service("rhOllamaServiceImpl")
public class OllamaServiceImpl implements IOllamaService {

    @Value("${ollama.api.url:http://localhost:11434/api/generate}")
    private String apiUrl;

    @Value("${ollama.model:tinyspring-rh-v2}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String generer(String prompt) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("prompt", prompt);
            body.put("stream", false);
            body.put("options", Map.of(
                    "temperature", 0.3,
                    "num_predict", 2048,
                    "top_p", 0.9
            ));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);

            if (response.getBody() != null) {
                String reponse = (String) response.getBody().get("response");
                if (reponse != null) {
                    return reponse.replace("### Response:", "").trim();
                }
            }

            return "Erreur : réponse vide d'Ollama.";

        } catch (Exception e) {
            System.err.println("❌ Erreur Ollama : " + e.getMessage());
            return "Erreur connexion Ollama. Vérifiez qu'Ollama tourne sur localhost:11434";
        }
    }
}
