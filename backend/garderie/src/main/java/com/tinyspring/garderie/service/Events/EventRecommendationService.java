package com.tinyspring.garderie.service.Events;

import com.tinyspring.garderie.dto.Events.EventRecommendationContextRequest;
import com.tinyspring.garderie.dto.Events.EventRecommendationResponse;

import java.util.List;

public interface EventRecommendationService {
    List<EventRecommendationResponse> recommendEvents(EventRecommendationContextRequest request);
}