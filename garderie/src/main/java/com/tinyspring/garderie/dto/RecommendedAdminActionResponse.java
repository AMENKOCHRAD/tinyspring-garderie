package com.tinyspring.garderie.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecommendedAdminActionResponse {

    private String recommendedService;
    private String recommendedUrgency;
    private String recommendedAction;
    private String recommendedDelay;
}