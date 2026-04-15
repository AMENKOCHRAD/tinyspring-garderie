package com.tinyspring.garderie.service;

import com.tinyspring.garderie.dto.MlPredictionRequest;
import com.tinyspring.garderie.dto.MlPredictionResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class MlPredictionService {

    private final RestTemplate restTemplate = new RestTemplate();

    public MlPredictionResponse predictCategory(String title, String description) {
        String url = "http://localhost:8000/predict";

        MlPredictionRequest request = new MlPredictionRequest(title, description);

        try {
            ResponseEntity<MlPredictionResponse> response = restTemplate.postForEntity(
                    url,
                    request,
                    MlPredictionResponse.class
            );

            return response.getBody();
        } catch (Exception e) {
            System.out.println("Erreur appel ML service: " + e.getMessage());
            return null;
        }
    }
}