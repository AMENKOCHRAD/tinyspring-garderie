package com.tinyspring.garderie.scheduler;

import com.tinyspring.garderie.dto.EscalationResult;
import com.tinyspring.garderie.dto.SmartPriorityResult;
import com.tinyspring.garderie.entity.Reclamation;
import com.tinyspring.garderie.entity.enums.ReclamationStatus;
import com.tinyspring.garderie.entity.enums.SmartPriorityLevel;
import com.tinyspring.garderie.repository.ReclamationRepository;
import com.tinyspring.garderie.service.EscalationService;
import com.tinyspring.garderie.service.SmartPriorityEngine;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReclamationSchedulerTest {

    private final ReclamationRepository repository = mock(ReclamationRepository.class);
    private final SmartPriorityEngine smartPriorityEngine = mock(SmartPriorityEngine.class);
    private final EscalationService escalationService = mock(EscalationService.class);
    private final ReclamationScheduler scheduler = new ReclamationScheduler(
            repository,
            smartPriorityEngine,
            escalationService
    );

    @Test
    void refreshActiveReclamationScoresSavesWhenSmartPriorityChanges() {
        Reclamation reclamation = new Reclamation();
        reclamation.setSmartPriorityScore(10);
        reclamation.setSmartPriorityLevel(SmartPriorityLevel.LOW);

        SmartPriorityResult newResult = SmartPriorityResult.builder()
                .totalScore(80)
                .level(SmartPriorityLevel.CRITICAL)
                .reason("Score recalculated")
                .build();

        when(repository.findActiveReclamationsSortedBySmartPriority(anyCollection()))
                .thenReturn(List.of(reclamation));
        when(smartPriorityEngine.calculate(reclamation)).thenReturn(newResult);
        when(escalationService.evaluate(reclamation)).thenReturn(
                EscalationResult.builder().escalationRequired(true).build()
        );

        scheduler.refreshActiveReclamationScores();

        verify(repository).save(reclamation);
        verify(escalationService).evaluate(reclamation);
    }

    @Test
    void refreshActiveReclamationScoresDoesNotSaveWhenSmartPriorityIsUnchanged() {
        Reclamation reclamation = new Reclamation();
        reclamation.setSmartPriorityScore(20);
        reclamation.setSmartPriorityLevel(SmartPriorityLevel.LOW);

        SmartPriorityResult sameResult = SmartPriorityResult.builder()
                .totalScore(20)
                .level(SmartPriorityLevel.LOW)
                .reason("Same score")
                .build();

        when(repository.findActiveReclamationsSortedBySmartPriority(anyCollection()))
                .thenReturn(List.of(reclamation));
        when(smartPriorityEngine.calculate(reclamation)).thenReturn(sameResult);
        when(escalationService.evaluate(reclamation)).thenReturn(
                EscalationResult.builder().escalationRequired(false).build()
        );

        scheduler.refreshActiveReclamationScores();

        verify(repository, never()).save(any(Reclamation.class));
    }

    @Test
    void logDailyReclamationSummaryReadsRepositoryCounters() {
        when(repository.countByStatus(ReclamationStatus.OPEN)).thenReturn(3L);
        when(repository.countByStatus(ReclamationStatus.IN_PROGRESS)).thenReturn(2L);
        when(repository.countByStatus(ReclamationStatus.RESOLVED)).thenReturn(5L);
        when(repository.countByStatus(ReclamationStatus.REJECTED)).thenReturn(1L);
        when(repository.countUnassignedActive(anyCollection())).thenReturn(4L);
        when(repository.countRecurringActive(anyCollection())).thenReturn(2L);

        scheduler.logDailyReclamationSummary();

        verify(repository).countByStatus(ReclamationStatus.OPEN);
        verify(repository).countByStatus(ReclamationStatus.IN_PROGRESS);
        verify(repository).countByStatus(ReclamationStatus.RESOLVED);
        verify(repository).countByStatus(ReclamationStatus.REJECTED);
        verify(repository).countUnassignedActive(anyCollection());
        verify(repository).countRecurringActive(anyCollection());
    }
}
