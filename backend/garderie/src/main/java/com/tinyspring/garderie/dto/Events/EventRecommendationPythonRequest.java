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
public class EventRecommendationPythonRequest {
    private String season;
    private String month;

    @JsonProperty("age_group")
    private String ageGroup;

    @JsonProperty("budget_level")
    private String budgetLevel;

    @JsonProperty("outdoor_preferred")
    private Boolean outdoorPreferred;

    @JsonProperty("city_context")
    private String cityContext;

    @JsonProperty("top_n")
    private Integer topN;

}
