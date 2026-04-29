package com.tinyspring.garderie.service.impl;

import com.tinyspring.garderie.dto.EscalationResult;
import com.tinyspring.garderie.entity.Reclamation;
import com.tinyspring.garderie.entity.enums.DecisionRecommendation;
import com.tinyspring.garderie.entity.enums.ReclamationCategory;
import com.tinyspring.garderie.entity.enums.ReclamationPriority;
import com.tinyspring.garderie.entity.enums.ReclamationStatus;
import com.tinyspring.garderie.entity.enums.SmartPriorityLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EscalationServiceImplTest {

    private final EscalationServiceImpl service = new EscalationServiceImpl();

    @Test
    void evaluateNullReclamationReturnsSafeDefault() {
        EscalationResult result = service.evaluate(null);

        assertFalse(result.isEscalationRequired());
        assertEquals("ADMINISTRATION", result.getRecommendedService());
        assertEquals("LOW", result.getRecommendedUrgency());
        assertEquals("NONE", result.getRecommendedDelay());
    }

    @Test
    void evaluateCriticalSecurityReclamationRequiresEscalation() {
        Reclamation reclamation = new Reclamation();
        reclamation.setCategory(ReclamationCategory.SECURITE);
        reclamation.setPriority(ReclamationPriority.HIGH);
        reclamation.setSmartPriorityLevel(SmartPriorityLevel.CRITICAL);
        reclamation.setSmartPriorityScore(90);

        EscalationResult result = service.evaluate(reclamation);

        assertTrue(result.isEscalationRequired());
        assertEquals("DIRECTION", result.getRecommendedService());
        assertEquals("CRITICAL", result.getRecommendedUrgency());
        assertEquals("IMMEDIATE", result.getRecommendedDelay());
    }

    @Test
    void evaluateImmediateDecisionRoutesToSpecializedService() {
        Reclamation reclamation = new Reclamation();
        reclamation.setCategory(ReclamationCategory.REPAS);
        reclamation.setPriority(ReclamationPriority.MEDIUM);
        reclamation.setDecisionRecommendation(DecisionRecommendation.MEDICAL_ATTENTION);
        reclamation.setSmartPriorityLevel(SmartPriorityLevel.MEDIUM);
        reclamation.setSmartPriorityScore(40);

        EscalationResult result = service.evaluate(reclamation);

        assertTrue(result.isEscalationRequired());
        assertEquals("SERVICE_MEDICAL", result.getRecommendedService());
        assertEquals("HIGH", result.getRecommendedUrgency());
        assertEquals("IMMEDIATE", result.getRecommendedDelay());
    }

    @Test
    void evaluateAndApplyUpdatesEscalationFieldsAndStatus() {
        Reclamation reclamation = new Reclamation();
        reclamation.setStatus(ReclamationStatus.OPEN);
        reclamation.setCategory(ReclamationCategory.TRANSPORT);
        reclamation.setPriority(ReclamationPriority.HIGH);
        reclamation.setDecisionRecommendation(DecisionRecommendation.TRANSPORT_ESCALATION);

        EscalationResult result = service.evaluateAndApply(reclamation);

        assertTrue(result.isEscalationRequired());
        assertTrue(reclamation.getAutoEscalated());
        assertEquals(ReclamationStatus.IN_PROGRESS, reclamation.getStatus());
        assertNotNull(reclamation.getEscalatedAt());
        assertNotNull(reclamation.getEscalationReason());
        assertEquals("SERVICE_TRANSPORT", reclamation.getRecommendedService());
    }

    @Test
    void evaluateAndApplyKeepsNonCriticalReclamationUnderSurveillance() {
        Reclamation reclamation = new Reclamation();
        reclamation.setStatus(ReclamationStatus.OPEN);
        reclamation.setCategory(ReclamationCategory.FINANCIER);
        reclamation.setPriority(ReclamationPriority.LOW);
        reclamation.setSmartPriorityLevel(SmartPriorityLevel.LOW);
        reclamation.setSmartPriorityScore(10);

        EscalationResult result = service.evaluateAndApply(reclamation);

        assertFalse(result.isEscalationRequired());
        assertFalse(reclamation.getAutoEscalated());
        assertEquals(ReclamationStatus.OPEN, reclamation.getStatus());
        assertEquals("SERVICE_FINANCIER", reclamation.getRecommendedService());
    }
}
