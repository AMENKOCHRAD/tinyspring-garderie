package com.tinyspring.garderie.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateReclamationRequest {

    private String title;

    private String description;

    // LOW, MEDIUM, HIGH
    private String priority;
}