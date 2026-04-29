package com.tinyspring.garderie.service.impl;

import com.tinyspring.garderie.dto.SmartPriorityResult;
import com.tinyspring.garderie.entity.Reclamation;
import com.tinyspring.garderie.entity.enums.DecisionRecommendation;
import com.tinyspring.garderie.entity.enums.ReclamationCategory;
import com.tinyspring.garderie.entity.enums.ReclamationPriority;
import com.tinyspring.garderie.entity.enums.SmartPriorityLevel;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmartPriorityEngineImplTest {

    private final SmartPriorityEngineImpl engine = new SmartPriorityEngineImpl(new SlaConfigServiceImpl());

    @Test
    void calculateReturnsCriticalScoreForUrgentRecurringSafetyReclamation() {
        Reclamation reclamation = new Reclamation();
        reclamation.setCreatedAt(LocalDateTime.now().minusHours(5));
        reclamation.setCategory(ReclamationCategory.SECURITE);
        reclamation.setPriority(ReclamationPriority.HIGH);
        reclamation.setRecurring(true);
        reclamation.setRecurrenceCount(5);
        reclamation.setDecisionRecommendation(DecisionRecommendation.MEDICAL_ATTENTION);

        SmartPriorityResult result = engine.calculate(reclamation);

        assertEquals(100, result.getTotalScore());
        assertEquals(SmartPriorityLevel.CRITICAL, result.getLevel());
        assertEquals(40, result.getPriorityPoints());
        assertEquals(30, result.getSlaPoints());
        assertEquals(15, result.getRecurrencePoints());
        assertEquals(10, result.getCategoryPoints());
        assertEquals(5, result.getDecisionPoints());
        assertTrue(result.isSlaBreached());
    }

    @Test
    void calculateUsesPredictedPriorityWhenHigherThanManualPriority() {
        Reclamation reclamation = new Reclamation();
        reclamation.setCreatedAt(LocalDateTime.now());
        reclamation.setCategory(ReclamationCategory.TRANSPORT);
        reclamation.setPriority(ReclamationPriority.LOW);
        reclamation.setPredictedPriority(ReclamationPriority.HIGH);

        SmartPriorityResult result = engine.calculate(reclamation);

        assertEquals(40, result.getPriorityPoints());
        assertEquals(5, result.getCategoryPoints());
        assertTrue(result.getReason().contains("(ML)"));
    }

    @Test
    void calculateReturnsLowLevelForRecentLowPriorityStandardReclamation() {
        Reclamation reclamation = new Reclamation();
        reclamation.setCreatedAt(LocalDateTime.now());
        reclamation.setCategory(ReclamationCategory.ADMINISTRATIF);
        reclamation.setPriority(ReclamationPriority.LOW);
        reclamation.setRecurring(false);

        SmartPriorityResult result = engine.calculate(reclamation);

        assertEquals(SmartPriorityLevel.LOW, result.getLevel());
        assertEquals(5, result.getPriorityPoints());
        assertEquals(2, result.getSlaPoints());
        assertEquals(0, result.getRecurrencePoints());
        assertEquals(0, result.getCategoryPoints());
        assertEquals(0, result.getDecisionPoints());
        assertFalse(result.isSlaBreached());
    }

    @Test
    void calculateAndApplyWritesScoreLevelAndReasonOnEntity() {
        Reclamation reclamation = new Reclamation();
        reclamation.setCreatedAt(LocalDateTime.now().minusHours(30));
        reclamation.setCategory(ReclamationCategory.HYGIENE);
        reclamation.setPriority(ReclamationPriority.MEDIUM);

        SmartPriorityResult result = engine.calculateAndApply(reclamation);

        assertEquals(result.getTotalScore(), reclamation.getSmartPriorityScore());
        assertEquals(result.getLevel(), reclamation.getSmartPriorityLevel());
        assertEquals(result.getReason(), reclamation.getSmartPriorityReason());
    }
}
