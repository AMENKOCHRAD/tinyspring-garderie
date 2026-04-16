package com.tinyspring.garderie.service.Events;

import com.tinyspring.garderie.dto.Events.EventRecommendationContextRequest;
import com.tinyspring.garderie.dto.Events.EventRecommendationPythonRequest;
import com.tinyspring.garderie.dto.Events.EventRecommendationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventRecommendationServiceImpl implements EventRecommendationService {

    private final RestTemplate restTemplate;

    @Value("${ai.service.events.url:http://localhost:5006}")
    private String pythonAiUrl;

    @Override
    public List<EventRecommendationResponse> recommendEvents(EventRecommendationContextRequest request) {
        try {
            EventRecommendationPythonRequest payload = EventRecommendationPythonRequest.builder()
                    .season(request.getSeason())
                    .month(request.getMonth())
                    .ageGroup(request.getAgeGroup())
                    .budgetLevel(request.getBudgetLevel())
                    .outdoorPreferred(Boolean.TRUE.equals(request.getOutdoorPreferred()))
                    .cityContext(request.getCityContext())
                    .topN(8)
                    .build();

            ResponseEntity<EventRecommendationResponse[]> response =
                    restTemplate.postForEntity(
                            pythonAiUrl + "/recommend-events",
                            payload,
                            EventRecommendationResponse[].class
                    );

            if (response.getBody() == null) {
                return List.of();
            }

            return List.of(response.getBody());

        } catch (Exception e) {
            throw new IllegalStateException("Impossible d'obtenir les recommandations IA", e);
        }
    }
}