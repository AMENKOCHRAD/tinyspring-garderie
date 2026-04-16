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

    @JsonProperty("location_name")
    private String locationName;

    private String city;
    private String country;

    private Double latitude;
    private Double longitude;

    @JsonProperty("indoor_outdoor")
    private String indoorOutdoor;

    @JsonProperty("requires_authorization")
    private boolean requiresAuthorization;

    @JsonProperty("suggested_price")
    private double suggestedPrice;

    @JsonProperty("predicted_label")
    private String predictedLabel;

    @JsonProperty("relevance_score")
    private double relevanceScore;

    @JsonProperty("venue_type")
    private String venueType;

    private String season;
    private String tags;

    @JsonProperty("match_reason")
    private String matchReason;
}