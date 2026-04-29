package com.tinyspring.garderie.service.impl;

import com.tinyspring.garderie.dto.SmartPriorityResult;
import com.tinyspring.garderie.entity.Reclamation;
import com.tinyspring.garderie.entity.enums.DecisionRecommendation;
import com.tinyspring.garderie.entity.enums.ReclamationCategory;
import com.tinyspring.garderie.entity.enums.ReclamationPriority;
import com.tinyspring.garderie.entity.enums.SmartPriorityLevel;
import com.tinyspring.garderie.service.SlaConfigService;
import com.tinyspring.garderie.service.SmartPriorityEngine;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * ════════════════════════════════════════════════════════════════════
 *   SMART PRIORITY ENGINE — Moteur de priorisation intelligente
 * ════════════════════════════════════════════════════════════════════
 *
 * Calcule un score composite (0 → 100) par réclamation pour permettre
 * au dashboard admin de classer automatiquement les réclamations
 * les plus urgentes en haut de liste.
 *
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  Composante          │ Max pts │ Critère                        │
 * ├─────────────────────────────────────────────────────────────────┤
 * │  1. Priorité         │   40    │ HIGH=40 / MEDIUM=20 / LOW=5    │
 * │  2. SLA              │   30    │ Selon avancement du délai SLA  │
 * │  3. Récurrence       │   15    │ Réclamation détectée récurrente │
 * │  4. Catégorie        │   10    │ Bonus catégories critiques      │
 * │  5. Décision urgente │    5    │ ML recommande action immédiate  │
 * ├─────────────────────────────────────────────────────────────────┤
 * │  TOTAL               │  100    │                                │
 * └─────────────────────────────────────────────────────────────────┘
 *
 * SmartPriorityLevel déduit du score final :
 *   CRITICAL  → 75 à 100  (rouge   — action immédiate requise)
 *   HIGH      → 50 à  74  (orange  — traitement urgent)
 *   MEDIUM    → 25 à  49  (jaune   — suivi normal)
 *   LOW       →  0 à  24  (vert    — file d'attente ordinaire)
 */
@Service
public class SmartPriorityEngineImpl implements SmartPriorityEngine {

    // ── Seuils de score → SmartPriorityLevel ─────────────────────────────
    private static final int CRITICAL_THRESHOLD = 75;
    private static final int HIGH_THRESHOLD     = 50;
    private static final int MEDIUM_THRESHOLD   = 25;

    // ── Points max par composante ─────────────────────────────────────────
    private static final int MAX_PRIORITY_POINTS  = 40;
    private static final int MAX_SLA_POINTS       = 30;
    private static final int MAX_RECURRENCE_POINTS = 15;
    private static final int MAX_CATEGORY_POINTS  = 10;
    private static final int MAX_DECISION_POINTS  =  5;

    // ── Catégories critiques (bonus maximum) ──────────────────────────────
    private static final Set<ReclamationCategory> CRITICAL_CATEGORIES = Set.of(
            ReclamationCategory.SECURITE,
            ReclamationCategory.COMPORTEMENT
    );

    // ── Catégories importantes (bonus partiel) ─────────────────────────────
    private static final Set<ReclamationCategory> IMPORTANT_CATEGORIES = Set.of(
            ReclamationCategory.TRANSPORT,
            ReclamationCategory.REPAS,
            ReclamationCategory.HYGIENE
    );

    // ── Décisions ML qui nécessitent une action immédiate ─────────────────
    private static final Set<DecisionRecommendation> IMMEDIATE_DECISIONS = Set.of(
            DecisionRecommendation.MEDICAL_ATTENTION,
            DecisionRecommendation.TRANSPORT_ESCALATION,
            DecisionRecommendation.INCREASE_SUPERVISION
    );

    private final SlaConfigService slaConfigService;

    public SmartPriorityEngineImpl(SlaConfigService slaConfigService) {
        this.slaConfigService = slaConfigService;
    }

    // ────────────────────────────────────────────────────────────────────────
    //   MÉTHODE PRINCIPALE : calculate()
    // ────────────────────────────────────────────────────────────────────────

    @Override
    public SmartPriorityResult calculate(Reclamation reclamation) {

        // ── 0. Calcul de l'âge de la réclamation ──────────────────────────
        LocalDateTime createdAt = reclamation.getCreatedAt() != null
                ? reclamation.getCreatedAt()
                : LocalDateTime.now();

        long ageInMinutes = ChronoUnit.MINUTES.between(createdAt, LocalDateTime.now());
        if (ageInMinutes < 0) {
            ageInMinutes = 0;
        }

        ReclamationCategory category = reclamation.getCategory() != null
                ? reclamation.getCategory()
                : ReclamationCategory.AUTRE;

        // ── 1. Points de priorité (max 40) ─────────────────────────────────
        int priorityPoints = calculatePriorityPoints(reclamation);

        // ── 2. Points SLA (max 30) ─────────────────────────────────────────
        int slaPoints = calculateSlaPoints(category, ageInMinutes);

        // ── 3. Points de récurrence (max 15) ───────────────────────────────
        int recurrencePoints = calculateRecurrencePoints(reclamation);

        // ── 4. Points de catégorie critique (max 10) ───────────────────────
        int categoryPoints = calculateCategoryPoints(category);

        // ── 5. Points de décision urgente — ML (max 5) ─────────────────────
        int decisionPoints = calculateDecisionPoints(reclamation);

        // ── Score total (plafonné à 100) ────────────────────────────────────
        int totalScore = Math.min(
                priorityPoints + slaPoints + recurrencePoints + categoryPoints + decisionPoints,
                100
        );

        // ── Niveau déduit du score ──────────────────────────────────────────
        SmartPriorityLevel level = resolveLevel(totalScore);

        // ── Infos SLA contextuelles ─────────────────────────────────────────
        int    slaHours          = slaConfigService.getSlaHours(category);
        double slaProgressPercent = slaConfigService.getSlaProgressPercent(category, ageInMinutes);
        String slaStatusLabel    = slaConfigService.getSlaStatusLabel(category, ageInMinutes);
        boolean slaBreached      = slaConfigService.isSlaBreached(category, ageInMinutes);

        // ── Construction de la raison lisible ──────────────────────────────
        String reason = buildReason(
                totalScore, level,
                priorityPoints, slaPoints, recurrencePoints, categoryPoints, decisionPoints,
                reclamation, slaStatusLabel
        );

        return SmartPriorityResult.builder()
                .totalScore(totalScore)
                .level(level)
                .reason(reason)
                .priorityPoints(priorityPoints)
                .slaPoints(slaPoints)
                .recurrencePoints(recurrencePoints)
                .categoryPoints(categoryPoints)
                .decisionPoints(decisionPoints)
                .ageInMinutes(ageInMinutes)
                .slaHours(slaHours)
                .slaProgressPercent(slaProgressPercent)
                .slaStatusLabel(slaStatusLabel)
                .slaBreached(slaBreached)
                .build();
    }

    // ────────────────────────────────────────────────────────────────────────
    //   calculateAndApply() — Calcule ET applique sur l'entité
    // ────────────────────────────────────────────────────────────────────────

    @Override
    public SmartPriorityResult calculateAndApply(Reclamation reclamation) {
        SmartPriorityResult result = calculate(reclamation);

        reclamation.setSmartPriorityScore(result.getTotalScore());
        reclamation.setSmartPriorityLevel(result.getLevel());
        reclamation.setSmartPriorityReason(result.getReason());

        return result;
    }

    // ────────────────────────────────────────────────────────────────────────
    //   COMPOSANTE 1 : Priorité (max 40 pts)
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Utilise la priorité effective (manuelle ou prédite par ML si plus haute).
     *
     * Règle de fusion :
     *  - Si la priorité ML prédite est plus haute que la manuelle → on prend la ML
     *  - Sinon on garde la priorité manuelle
     * Cela permet d'éviter de sous-estimer une réclamation validée par l'IA.
     */
    private int calculatePriorityPoints(Reclamation reclamation) {
        ReclamationPriority effectivePriority = resolveEffectivePriority(reclamation);

        if (effectivePriority == null) {
            return 5; // Défaut LOW si aucune priorité définie
        }

        return switch (effectivePriority) {
            case HIGH   -> MAX_PRIORITY_POINTS;      // 40 pts
            case MEDIUM -> MAX_PRIORITY_POINTS / 2;  // 20 pts
            case LOW    -> 5;                        //  5 pts
        };
    }

    /**
     * Résout la priorité effective en combinant priorité manuelle et ML.
     * La ML peut "upgrader" la priorité si elle détecte quelque chose de plus urgent.
     */
    private ReclamationPriority resolveEffectivePriority(Reclamation reclamation) {
        ReclamationPriority manual    = reclamation.getPriority();
        ReclamationPriority predicted = reclamation.getPredictedPriority();

        if (manual == null && predicted == null) {
            return ReclamationPriority.MEDIUM; // Défaut prudent
        }

        if (manual == null)    return predicted;
        if (predicted == null) return manual;

        // Prendre la plus élevée des deux
        int manualRank    = priorityRank(manual);
        int predictedRank = priorityRank(predicted);

        return predictedRank > manualRank ? predicted : manual;
    }

    /** Rang numérique de priorité pour comparaison (HIGH=3, MEDIUM=2, LOW=1) */
    private int priorityRank(ReclamationPriority priority) {
        return switch (priority) {
            case HIGH   -> 3;
            case MEDIUM -> 2;
            case LOW    -> 1;
        };
    }

    // ────────────────────────────────────────────────────────────────────────
    //   COMPOSANTE 2 : SLA (max 30 pts)
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Plus le SLA s'approche (ou dépasse) son délai, plus les points montent.
     *
     *  SLA dépassé  (≥ 100%) → 30 pts  (urgence maximale)
     *  SLA critique (≥  75%) → 22 pts
     *  SLA en danger(≥  50%) → 14 pts
     *  SLA en danger(≥  25%) →  7 pts
     *  SLA OK       (<  25%) →  2 pts  (réclamation très récente)
     */
    private int calculateSlaPoints(ReclamationCategory category, long ageInMinutes) {
        double progress = slaConfigService.getSlaProgressPercent(category, ageInMinutes);

        if (progress >= 100.0) return MAX_SLA_POINTS;       // 30 pts — SLA DÉPASSÉ
        if (progress >=  75.0) return 22;                   // Critique
        if (progress >=  50.0) return 14;                   // En danger
        if (progress >=  25.0) return  7;                   // Attention
        return 2;                                            // Très récent
    }

    // ────────────────────────────────────────────────────────────────────────
    //   COMPOSANTE 3 : Récurrence (max 15 pts)
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Une réclamation récurrente signale un problème systémique non résolu.
     * Plus il y a de récurrences, plus les points augmentent (jusqu'au max).
     *
     *  récurrente + recurrenceCount >= 5  → 15 pts
     *  récurrente + recurrenceCount >= 3  → 10 pts
     *  récurrente (peu importe count)     →  7 pts
     *  non récurrente                     →  0 pts
     */
    private int calculateRecurrencePoints(Reclamation reclamation) {
        if (!Boolean.TRUE.equals(reclamation.getRecurring())) {
            return 0;
        }

        int count = reclamation.getRecurrenceCount() != null
                ? reclamation.getRecurrenceCount()
                : 1;

        if (count >= 5) return MAX_RECURRENCE_POINTS; // 15 pts — problème chronique
        if (count >= 3) return 10;                    // Problème répété
        return 7;                                     // Première récurrence
    }

    // ────────────────────────────────────────────────────────────────────────
    //   COMPOSANTE 4 : Catégorie critique (max 10 pts)
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Certaines catégories impliquent des risques pour la sécurité ou la santé
     * des enfants et méritent un bonus systématique.
     *
     *  Critique  (SECURITE, COMPORTEMENT) → 10 pts
     *  Importante (TRANSPORT, REPAS, HYGIENE) →  5 pts
     *  Standard                             →  0 pts
     */
    private int calculateCategoryPoints(ReclamationCategory category) {
        if (category == null) return 0;

        if (CRITICAL_CATEGORIES.contains(category))  return MAX_CATEGORY_POINTS; // 10 pts
        if (IMPORTANT_CATEGORIES.contains(category)) return 5;                   //  5 pts

        return 0;
    }

    // ────────────────────────────────────────────────────────────────────────
    //   COMPOSANTE 5 : Décision urgente ML (max 5 pts)
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Si le modèle ML recommande une action immédiate (médicale, transport,
     * surveillance renforcée), on ajoute un bonus pour signaler l'urgence.
     */
    private int calculateDecisionPoints(Reclamation reclamation) {
        DecisionRecommendation decision = reclamation.getDecisionRecommendation();

        if (decision != null && IMMEDIATE_DECISIONS.contains(decision)) {
            return MAX_DECISION_POINTS; // 5 pts
        }

        return 0;
    }

    // ────────────────────────────────────────────────────────────────────────
    //   Résolution du niveau SmartPriorityLevel
    // ────────────────────────────────────────────────────────────────────────

    private SmartPriorityLevel resolveLevel(int totalScore) {
        if (totalScore >= CRITICAL_THRESHOLD) return SmartPriorityLevel.CRITICAL;
        if (totalScore >= HIGH_THRESHOLD)     return SmartPriorityLevel.HIGH;
        if (totalScore >= MEDIUM_THRESHOLD)   return SmartPriorityLevel.MEDIUM;
        return SmartPriorityLevel.LOW;
    }

    // ────────────────────────────────────────────────────────────────────────
    //   Construction de la raison lisible (pour historique et dashboard)
    // ────────────────────────────────────────────────────────────────────────

    private String buildReason(int totalScore,
                               SmartPriorityLevel level,
                               int priorityPoints,
                               int slaPoints,
                               int recurrencePoints,
                               int categoryPoints,
                               int decisionPoints,
                               Reclamation reclamation,
                               String slaStatusLabel) {

        List<String> parts = new ArrayList<>();

        // Priorité effective
        ReclamationPriority effectivePriority = resolveEffectivePriority(reclamation);
        if (effectivePriority != null) {
            String source = reclamation.getPredictedPriority() != null
                    && priorityRank(reclamation.getPredictedPriority()) > priorityRank(
                            reclamation.getPriority() != null
                                    ? reclamation.getPriority()
                                    : ReclamationPriority.LOW)
                    ? " (ML)" : "";
            parts.add("Priorité " + effectivePriority.name() + source + " → +" + priorityPoints + " pts");
        }

        // SLA
        parts.add(slaStatusLabel + " → +" + slaPoints + " pts");

        // Récurrence
        if (recurrencePoints > 0) {
            int count = reclamation.getRecurrenceCount() != null ? reclamation.getRecurrenceCount() : 1;
            parts.add("Réclamation récurrente (" + count + "x) → +" + recurrencePoints + " pts");
        }

        // Catégorie
        if (categoryPoints > 0) {
            ReclamationCategory cat = reclamation.getCategory();
            parts.add("Catégorie critique " + (cat != null ? cat.name() : "") + " → +" + categoryPoints + " pts");
        }

        // Décision ML urgente
        if (decisionPoints > 0) {
            DecisionRecommendation decision = reclamation.getDecisionRecommendation();
            parts.add("Décision ML urgente " + (decision != null ? decision.name() : "") + " → +" + decisionPoints + " pts");
        }

        String detailLine = String.join(" | ", parts);

        return "Score " + totalScore + "/100 [" + level.name() + "] — " + detailLine;
    }
}
