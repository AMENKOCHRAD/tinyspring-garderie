package com.tinyspring.garderie.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Résultat de l'analyse d'escalade automatique.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EscalationResult {

    private boolean escalationRequired;

    private String recommendedService;

    private String recommendedUrgency;

    private String recommendedDelay;

    private String recommendedAction;

    private String escalationReason;
}