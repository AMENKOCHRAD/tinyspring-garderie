package com.tinyspring.garderie.service;

import com.tinyspring.garderie.dto.MlDecisionRequest;
import com.tinyspring.garderie.dto.MlPredictionRequest;
import com.tinyspring.garderie.dto.MlPredictionResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class MlPredictionService {

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String BASE_URL = "http://localhost:8000";
    private static final String CATEGORY_URL = BASE_URL + "/predict-category";
    private static final String PRIORITY_URL = BASE_URL + "/predict-priority";
    private static final String DECISION_URL = BASE_URL + "/predict-decision";

    public MlPredictionResponse predictCategory(String title, String description) {
        MlPredictionRequest request = new MlPredictionRequest(title, description);

        try {
            ResponseEntity<MlPredictionResponse> response = restTemplate.postForEntity(
                    CATEGORY_URL,
                    request,
                    MlPredictionResponse.class
            );
            return response.getBody();
        } catch (Exception e) {
            System.out.println("Erreur appel ML category service: " + e.getMessage());
            return null;
        }
    }

    public MlPredictionResponse predictPriority(String title, String description) {
        MlPredictionRequest request = new MlPredictionRequest(title, description);

        try {
            ResponseEntity<MlPredictionResponse> response = restTemplate.postForEntity(
                    PRIORITY_URL,
                    request,
                    MlPredictionResponse.class
            );
            return response.getBody();
        } catch (Exception e) {
            System.out.println("Erreur appel ML priority service: " + e.getMessage());
            return null;
        }
    }

    public MlPredictionResponse predictDecision(String title, String description, String category, String priority) {
        MlDecisionRequest request = new MlDecisionRequest(title, description, category, priority);

        try {
            ResponseEntity<MlPredictionResponse> response = restTemplate.postForEntity(
                    DECISION_URL,
                    request,
                    MlPredictionResponse.class
            );
            return response.getBody();
        } catch (Exception e) {
            System.out.println("Erreur appel ML decision service: " + e.getMessage());
            return null;
        }
    }
}