package com.tinyspring.garderie.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Informations d'escalade automatique affichées côté admin.
 *
 * Ce DTO permet au frontend de comprendre :
 * - est-ce que la réclamation est escaladée ?
 * - pourquoi ?
 * - vers quel service ?
 * - avec quelle urgence ?
 * - quelle action admin doit être faite ?
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EscalationInfoResponse {

    private Long reclamationId;

    private Boolean autoEscalated;

    private LocalDateTime escalatedAt;

    private String escalationReason;

    private String recommendedService;

    private String recommendedUrgency;

    private String recommendedDelay;

    private String recommendedAction;

    private Integer smartPriorityScore;

    private String smartPriorityLevel;

    private String smartPriorityReason;

    private Boolean recurring;

    private Integer recurrenceCount;

    private String status;
}
