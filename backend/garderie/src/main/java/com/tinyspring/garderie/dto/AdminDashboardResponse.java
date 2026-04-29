package com.tinyspring.garderie.dto;

import com.tinyspring.garderie.entity.Reclamation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * Réponse complète du dashboard intelligent pour l'admin.
 *
 * Contient :
 *  - La liste des réclamations triées par smartPriorityScore DESC
 *  - Les statistiques globales (par statut, par niveau SLA, par catégorie)
 *  - Les compteurs d'alertes (SLA dépassés, CRITICAL en attente)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDashboardResponse {

    // ── Liste principale (triée par score décroissant) ────────────────────

    /** Réclamations actives (OPEN + IN_PROGRESS) triées par smartPriorityScore DESC */
    private List<Reclamation> prioritizedReclamations;

    // ── Compteurs d'alertes immédiates ────────────────────────────────────

    /** Nombre total de réclamations actives (OPEN + IN_PROGRESS) */
    private int totalActive;

    /** Nombre de réclamations en niveau CRITICAL (score >= 75) */
    private int criticalCount;

    /** Nombre de réclamations en niveau HIGH (score 50-74) */
    private int highCount;

    /** Nombre de réclamations en niveau MEDIUM (score 25-49) */
    private int mediumCount;

    /** Nombre de réclamations en niveau LOW (score 0-24) */
    private int lowCount;

    /** Nombre de réclamations dont le SLA est dépassé (score SLA = max) */
    private int slaBreachedCount;

    /** Nombre de réclamations récurrentes actives */
    private int recurringCount;

    /** Nombre de réclamations sans admin assigné */
    private int unassignedCount;

    // ── Statistiques par statut ───────────────────────────────────────────

    /** Nombre de réclamations OPEN */
    private int openCount;

    /** Nombre de réclamations IN_PROGRESS */
    private int inProgressCount;

    /** Nombre de réclamations RESOLVED (total historique) */
    private int resolvedCount;

    /** Nombre de réclamations REJECTED (total historique) */
    private int rejectedCount;

    // ── Statistiques par catégorie ────────────────────────────────────────

    /** Répartition des réclamations actives par catégorie → ex: {SECURITE:3, REPAS:5} */
    private Map<String, Long> activeByCategory;

    // ── Score moyen ────────────────────────────────────────────────────────

    /** Score de priorité moyen des réclamations actives */
    private double averageSmartScore;

    /** Score maximum parmi les réclamations actives */
    private int maxSmartScore;
}
