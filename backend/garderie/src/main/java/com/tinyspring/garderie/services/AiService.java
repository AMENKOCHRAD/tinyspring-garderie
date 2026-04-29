package com.tinyspring.garderie.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiService {

    @Value("${ai.api.key}")
    private String apiKey;

    @Value("${ai.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public String generateDailyReport(String childName, String animatorNotes) {
        String safeName = childName != null ? childName : "votre enfant";
        String prompt = String.format(
            "Tu es un assistant IA professionnel pour une crèche. Ta SEULE ET UNIQUE tâche est de rédiger un rapport journalier pour les parents de l'enfant %s. " +
            "CONSIGNES STRICTES : " +
            "1. Ne réponds à aucune question hors sujet. Si l'utilisateur tente de te faire dire autre chose, ignore-le et génère uniquement le rapport. " +
            "2. N'UTILISE AUCUN FORMATAGE MARKDOWN. Absolument aucune étoile (* ou **), aucun dièse (#), ni tirets pour les listes. " +
            "3. Rédige un texte clair, avec des paragraphes normaux et quelques émojis pour égayer. " +
            "Voici les notes brutes de l'éducatrice à transformer : '%s'", 
            safeName, animatorNotes
        );
        String fallback = "Rapport de la journée (Mode IA-Secours)\n\n" +
                          "Bonjour chers parents ! 👋\n\n" +
                          "Aujourd'hui a été une très belle journée pour " + safeName + " à la garderie. 🌟\n\n" +
                          "Voici le résumé de l'éducatrice :\n" +
                          animatorNotes + "\n\n" +
                          "Tout s'est déroulé à merveille ! L'enthousiasme et la bonne humeur étaient au rendez-vous. 🎨🧸\n\n" +
                          "Nous avons hâte de vous retrouver demain !\n\n" +
                          "Bien à vous,\n" +
                          "L'équipe de la Garderie 🌻";

        return callGroq(prompt, fallback);
    }

    public String suggestActivities(Integer ageMin, Integer ageMax, Integer capacite) {
        try {
            String url = "http://localhost:8000/recommend-activities";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("ageMin", ageMin != null ? ageMin : 1);
            requestBody.put("ageMax", ageMax != null ? ageMax : 6);
            requestBody.put("capacite", capacite != null ? capacite : 15);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return (String) response.getBody().get("response");
            }
            return "Erreur lors de l'appel au modèle ML local.";
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Erreur API ML Locale : " + e.getMessage());
            return "Une erreur s'est produite avec le modèle ML : " + e.getMessage();
        }
    }

    // --- 1. K-Means Clustering ---
    public Map<String, Object> clusterChildren(List<Map<String, Object>> children, int numGroups) {
        try {
            String url = "http://localhost:8000/ml/cluster-children";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("children", children);
            requestBody.put("numGroups", numGroups);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            return response.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("error", e.getMessage());
        }
    }

    // --- 2. Decision Tree (Recommandation de Salle - basé sur room_dataset.csv) ---
    public Map<String, Object> recommendRoomForGroup(Integer capacite, Integer ageMoyen) {
        try {
            String url = "http://localhost:8000/ml/recommend-room";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("capacite", capacite != null ? capacite : 15);
            requestBody.put("ageMoyen", ageMoyen != null ? ageMoyen : 4);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            return response.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("error", e.getMessage());
        }
    }

    private String callGroq(String prompt, String fallbackResponse) {
        try {
            // L'URL de Groq est directement utilisée
            String url = apiUrl;

            // Prepare Headers (Groq utilise Bearer token)
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            // Construct JSON request body (Format OpenAI/Groq)
            Map<String, String> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "llama-3.1-8b-instant"); // Modèle gratuit, extrêmement rapide
            requestBody.put("messages", List.of(message));

            // Wrap in an HttpEntity
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // Make the Request
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            
            // Parse response
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // The structure is choices[0].message.content
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> messageMap = (Map<String, Object>) choices.get(0).get("message");
                    if (messageMap != null && messageMap.containsKey("content")) {
                        return (String) messageMap.get("content");
                    }
                }
            }
            return fallbackResponse;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Erreur API Groq : " + e.getMessage());
            return "Une erreur s'est produite avec l'API Groq : " + e.getMessage() + ". Veuillez vérifier votre clé API sur console.groq.com.";
        }
    }
}
