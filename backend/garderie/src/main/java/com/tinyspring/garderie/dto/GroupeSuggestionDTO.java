package com.tinyspring.garderie.dto;

import com.tinyspring.garderie.entity.Groupe;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupeSuggestionDTO {
    private Groupe groupe;
    private Double matchPercentage;
    private Integer availableSeats;
    private List<String> matchReasons;
}
