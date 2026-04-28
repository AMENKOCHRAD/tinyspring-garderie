package com.tinyspring.garderie.dto;

import com.tinyspring.garderie.entity.enums.SmartPriorityLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Résultat complet du calcul de priorité intelligente.
 * Contient le score final, le niveau, la raison détaillée,
 * et le détail point par point pour la transparence.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmartPriorityResult {

    /** Score total calculé (0 à 100) */
    private int totalScore;

    /** Niveau de priorité intelligente déduit du score */
    private SmartPriorityLevel level;

    /**
     * Explication lisible de la priorité calculée.
     * Ex: "Score 87/100 — Priorité HIGH (+40), SLA DÉPASSÉ (+30), Réclamation récurrente (+15), Catégorie SECURITE (+10), Décision immédiate (+5)"
     */
    private String reason;

    // ── Détail par composante ─────────────────────────────────────────────

    /** Points attribués pour la priorité (manuelle ou ML) */
    private int priorityPoints;

    /** Points attribués pour l'avancement du SLA */
    private int slaPoints;

    /** Points attribués pour la récurrence */
    private int recurrencePoints;

    /** Points attribués pour la catégorie critique */
    private int categoryPoints;

    /** Points attribués pour la décision urgente (ML) */
    private int decisionPoints;

    // ── Infos SLA contextuelles ───────────────────────────────────────────

    /** Âge de la réclamation en minutes au moment du calcul */
    private long ageInMinutes;

    /** Délai SLA de la catégorie en heures */
    private int slaHours;

    /** Pourcentage d'avancement du SLA (peut dépasser 100%) */
    private double slaProgressPercent;

    /** Label lisible du statut SLA */
    private String slaStatusLabel;

    /** Indique si le SLA est dépassé */
    private boolean slaBreached;
}
