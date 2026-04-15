package com.tinyspring.garderie.dto.Events;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRecommendationResponse {
    private String title;
    private String description;

    @JsonProperty("event_type")
    private String eventType;

    @JsonProperty("suggested_location")
    private String suggestedLocation;

    @JsonProperty("indoor_outdoor")
    private String indoorOutdoor;

    @JsonProperty("requires_authorization")
    private Boolean requiresAuthorization;

    @JsonProperty("suggested_price")
    private Double suggestedPrice;

    @JsonProperty("predicted_label")
    private String predictedLabel;

    @JsonProperty("relevance_score")
    private Double relevanceScore;
}