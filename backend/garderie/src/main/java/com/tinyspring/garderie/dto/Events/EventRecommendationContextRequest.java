package com.tinyspring.garderie.dto.Events;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRecommendationContextRequest {
    private String season;
    private String month;
    private String ageGroup;
    private String budgetLevel;
    private Boolean outdoorPreferred;
    private String cityContext;
}
