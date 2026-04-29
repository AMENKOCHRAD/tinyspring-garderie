package com.tinyspring.garderie.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MlPredictionRequest {
    private String title;
    private String description;
}