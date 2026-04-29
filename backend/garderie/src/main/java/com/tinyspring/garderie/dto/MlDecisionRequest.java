package com.tinyspring.garderie.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MlDecisionRequest {
    private String title;
    private String description;
    private String category;
    private String priority;
}