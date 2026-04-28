package com.tinyspring.garderie.service.impl;

import com.tinyspring.garderie.dto.EscalationResult;
import com.tinyspring.garderie.entity.Reclamation;
import com.tinyspring.garderie.entity.enums.DecisionRecommendation;
import com.tinyspring.garderie.entity.enums.ReclamationCategory;
import com.tinyspring.garderie.entity.enums.ReclamationPriority;
import com.tinyspring.garderie.entity.enums.ReclamationStatus;
import com.tinyspring.garderie.entity.enums.SmartPriorityLevel;
import com.tinyspring.garderie.service.EscalationService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service métier responsable de l'escalade automatique intelligente.
 *
 * Objectif :
 * - détecter les réclamations critiques
 * - recommander le service concerné
 * - recommander une action concrète
 * - automatiser une partie du travail de l'admin
 */
@Service
public class EscalationServiceImpl implements EscalationService {

    @Override
    public EscalationResult evaluate(Reclamation reclamation) {
        if (reclamation == null) {
            return EscalationResult.builder()
                    .escalationRequired(false)
                    .recommendedService("ADMINISTRATION")
                    .recommendedUrgency("LOW")
                    .recommendedDelay("NONE")
                    .recommendedAction("Aucune réclamation à analyser.")
                    .escalationReason("Réclamation inexistante.")
                    .build();
        }

        List<String> reasons = new ArrayList<>();

        boolean escalationRequired = false;

        SmartPriorityLevel smartLevel = reclamation.getSmartPriorityLevel();
        Integer smartScore = reclamation.getSmartPriorityScore();
        DecisionRecommendation decision = reclamation.getDecisionRecommendation();
        ReclamationCategory category = reclamation.getCategory();
        ReclamationPriority priority = resolveEffectivePriority(reclamation);

        // Règle 1 : niveau critique
        if (smartLevel == SmartPriorityLevel.CRITICAL) {
            escalationRequired = true;
            reasons.add("Niveau de priorité intelligente CRITICAL");
        }

        // Règle 2 : score très élevé
        if (smartScore != null && smartScore >= 75) {
            escalationRequired = true;
            reasons.add("Score intelligent élevé (" + smartScore + "/100)");
        }

        // Règle 3 : décision urgente ML
        if (isImmediateDecision(decision)) {
            escalationRequired = true;
            reasons.add("Décision ML urgente : " + decision.name());
        }

        // Règle 4 : réclamation récurrente
        if (Boolean.TRUE.equals(reclamation.getRecurring())
                && reclamation.getRecurrenceCount() != null
                && reclamation.getRecurrenceCount() >= 3) {
            escalationRequired = true;
            reasons.add("Réclamation récurrente (" + reclamation.getRecurrenceCount() + " cas similaires)");
        }

        // Règle 5 : sécurité + priorité haute
        if (category == ReclamationCategory.SECURITE && priority == ReclamationPriority.HIGH) {
            escalationRequired = true;
            reasons.add("Réclamation sécurité avec priorité HIGH");
        }

        // Règle 6 : catégorie sensible + décision non vide
        if (isSensitiveCategory(category) && decision != null) {
            escalationRequired = true;
            reasons.add("Catégorie sensible avec décision recommandée");
        }

        String recommendedService = resolveRecommendedService(reclamation);
        String urgency = resolveUrgency(reclamation, escalationRequired);
        String delay = resolveDelay(urgency, decision);
        String action = resolveRecommendedAction(reclamation, recommendedService, urgency);

        if (!escalationRequired) {
            reasons.add("Aucune règle d'escalade critique détectée");
        }

        return EscalationResult.builder()
                .escalationRequired(escalationRequired)
                .recommendedService(recommendedService)
                .recommendedUrgency(urgency)
                .recommendedDelay(delay)
                .recommendedAction(action)
                .escalationReason(String.join(" | ", reasons))
                .build();
    }

    @Override
    public EscalationResult evaluateAndApply(Reclamation reclamation) {
        EscalationResult result = evaluate(reclamation);

        if (reclamation == null) {
            return result;
        }

        if (result.isEscalationRequired()) {
            reclamation.setAutoEscalated(true);
            reclamation.setEscalatedAt(LocalDateTime.now());
            reclamation.setEscalationReason(result.getEscalationReason());
            reclamation.setRecommendedService(result.getRecommendedService());
            reclamation.setRecommendedDelay(result.getRecommendedDelay());
            reclamation.setRecommendedAction(result.getRecommendedAction());

            if (reclamation.getStatus() == ReclamationStatus.OPEN) {
                reclamation.setStatus(ReclamationStatus.IN_PROGRESS);
            }
        } else {
            if (reclamation.getAutoEscalated() == null) {
                reclamation.setAutoEscalated(false);
            }

            reclamation.setRecommendedService(result.getRecommendedService());
            reclamation.setRecommendedDelay(result.getRecommendedDelay());
            reclamation.setRecommendedAction(result.getRecommendedAction());
        }

        return result;
    }

    private boolean isImmediateDecision(DecisionRecommendation decision) {
        if (decision == null) {
            return false;
        }

        return decision == DecisionRecommendation.MEDICAL_ATTENTION
                || decision == DecisionRecommendation.TRANSPORT_ESCALATION
                || decision == DecisionRecommendation.INCREASE_SUPERVISION;
    }

    private boolean isSensitiveCategory(ReclamationCategory category) {
        if (category == null) {
            return false;
        }

        return category == ReclamationCategory.SECURITE
                || category == ReclamationCategory.REPAS
                || category == ReclamationCategory.TRANSPORT
                || category == ReclamationCategory.HYGIENE
                || category == ReclamationCategory.COMPORTEMENT;
    }

    private ReclamationPriority resolveEffectivePriority(Reclamation reclamation) {
        ReclamationPriority manual = reclamation.getPriority();
        ReclamationPriority predicted = reclamation.getPredictedPriority();

        if (manual == null && predicted == null) {
            return ReclamationPriority.MEDIUM;
        }

        if (manual == null) {
            return predicted;
        }

        if (predicted == null) {
            return manual;
        }

        return priorityRank(predicted) > priorityRank(manual) ? predicted : manual;
    }

    private int priorityRank(ReclamationPriority priority) {
        if (priority == null) {
            return 0;
        }

        return switch (priority) {
            case LOW -> 1;
            case MEDIUM -> 2;
            case HIGH -> 3;
        };
    }

    private String resolveRecommendedService(Reclamation reclamation) {
        DecisionRecommendation decision = reclamation.getDecisionRecommendation();

        if (decision != null) {
            return switch (decision) {
                case MEDICAL_ATTENTION -> "SERVICE_MEDICAL";
                case TRANSPORT_ESCALATION -> "SERVICE_TRANSPORT";
                case INCREASE_SUPERVISION -> "SERVICE_PEDAGOGIQUE";
                case REPAIR_NEEDED -> "MAINTENANCE";
                case STAFF_TRAINING -> "RESSOURCES_HUMAINES";
                case PROCESS_IMPROVEMENT -> "ADMINISTRATION";
                case ADMINISTRATIVE_CORRECTION -> "SERVICE_ADMINISTRATIF";
                case PARENT_FOLLOWUP -> "RELATION_PARENT";
            };
        }

        ReclamationCategory category = reclamation.getCategory();

        if (category == null) {
            return "ADMINISTRATION";
        }

        return switch (category) {
            case SECURITE -> "DIRECTION";
            case REPAS -> "SERVICE_MEDICAL";
            case TRANSPORT -> "SERVICE_TRANSPORT";
            case COMPORTEMENT -> "SERVICE_PEDAGOGIQUE";
            case HYGIENE -> "SERVICE_HYGIENE";
            case PERSONNEL -> "RESSOURCES_HUMAINES";
            case FINANCIER -> "SERVICE_FINANCIER";
            case PEDAGOGIQUE -> "SERVICE_PEDAGOGIQUE";
            case ADMINISTRATIF -> "SERVICE_ADMINISTRATIF";
            case AUTRE -> "ADMINISTRATION";
        };
    }

    private String resolveUrgency(Reclamation reclamation, boolean escalationRequired) {
        if (!escalationRequired) {
            return "LOW";
        }

        if (reclamation.getSmartPriorityLevel() == SmartPriorityLevel.CRITICAL) {
            return "CRITICAL";
        }

        if (reclamation.getSmartPriorityLevel() == SmartPriorityLevel.HIGH) {
            return "HIGH";
        }

        if (isImmediateDecision(reclamation.getDecisionRecommendation())) {
            return "HIGH";
        }

        if (Boolean.TRUE.equals(reclamation.getRecurring())) {
            return "MEDIUM";
        }

        return "MEDIUM";
    }

    private String resolveDelay(String urgency, DecisionRecommendation decision) {
        if (decision == DecisionRecommendation.MEDICAL_ATTENTION
                || decision == DecisionRecommendation.TRANSPORT_ESCALATION
                || decision == DecisionRecommendation.INCREASE_SUPERVISION) {
            return "IMMEDIATE";
        }

        return switch (urgency) {
            case "CRITICAL" -> "IMMEDIATE";
            case "HIGH" -> "24H";
            case "MEDIUM" -> "48H";
            default -> "NONE";
        };
    }

    private String resolveRecommendedAction(Reclamation reclamation,
                                            String recommendedService,
                                            String urgency) {
        DecisionRecommendation decision = reclamation.getDecisionRecommendation();

        if (decision != null) {
            return switch (decision) {
                case MEDICAL_ATTENTION ->
                        "Vérifier immédiatement l'état de l'enfant, informer la direction et contacter le parent si nécessaire.";
                case TRANSPORT_ESCALATION ->
                        "Contacter immédiatement le responsable transport et vérifier l'incident signalé.";
                case INCREASE_SUPERVISION ->
                        "Renforcer la surveillance et demander un retour rapide à l'équipe pédagogique.";
                case REPAIR_NEEDED ->
                        "Sécuriser la zone concernée et planifier une intervention de maintenance.";
                case STAFF_TRAINING ->
                        "Identifier le personnel concerné et prévoir une action de sensibilisation ou formation.";
                case PROCESS_IMPROVEMENT ->
                        "Analyser le processus interne concerné et proposer une amélioration organisationnelle.";
                case ADMINISTRATIVE_CORRECTION ->
                        "Vérifier le dossier administratif et corriger les informations nécessaires.";
                case PARENT_FOLLOWUP ->
                        "Contacter le parent pour obtenir des précisions complémentaires.";
            };
        }

        return "Transférer la réclamation vers " + recommendedService
                + " avec une urgence " + urgency
                + " et assurer un suivi dans le délai recommandé.";
    }
}