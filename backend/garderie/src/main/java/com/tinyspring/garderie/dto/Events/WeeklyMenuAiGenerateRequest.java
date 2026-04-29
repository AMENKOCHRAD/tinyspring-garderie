package com.tinyspring.garderie.dto.Events;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyMenuAiGenerateRequest {
    @NotBlank
    private String weekStartDate;
}