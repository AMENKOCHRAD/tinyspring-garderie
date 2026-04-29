package com.tinyspring.garderie.scheduler;

import com.tinyspring.garderie.dto.EscalationResult;
import com.tinyspring.garderie.dto.SmartPriorityResult;
import com.tinyspring.garderie.entity.Reclamation;
import com.tinyspring.garderie.entity.enums.ReclamationStatus;
import com.tinyspring.garderie.repository.ReclamationRepository;
import com.tinyspring.garderie.service.EscalationService;
import com.tinyspring.garderie.service.SmartPriorityEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Component
@Slf4j
public class ReclamationScheduler {

    private static final List<ReclamationStatus> ACTIVE_STATUSES = List.of(
            ReclamationStatus.OPEN,
            ReclamationStatus.IN_PROGRESS
    );

    private final ReclamationRepository reclamationRepository;
    private final SmartPriorityEngine smartPriorityEngine;
    private final EscalationService escalationService;

    public ReclamationScheduler(ReclamationRepository reclamationRepository,
                                SmartPriorityEngine smartPriorityEngine,
                                EscalationService escalationService) {
        this.reclamationRepository = reclamationRepository;
        this.smartPriorityEngine = smartPriorityEngine;
        this.escalationService = escalationService;
    }

    @Scheduled(
            fixedDelayString = "${tinyspring.scheduler.reclamation-refresh-delay-ms:300000}",
            initialDelayString = "${tinyspring.scheduler.initial-delay-ms:60000}"
    )
    @Transactional
    public void refreshActiveReclamationScores() {
        List<Reclamation> activeReclamations =
                reclamationRepository.findActiveReclamationsSortedBySmartPriority(ACTIVE_STATUSES);

        int updatedCount = 0;
        int escalationCandidateCount = 0;

        for (Reclamation reclamation : activeReclamations) {
            SmartPriorityResult result = smartPriorityEngine.calculate(reclamation);

            if (isSmartPriorityChanged(reclamation, result)) {
                reclamation.setSmartPriorityScore(result.getTotalScore());
                reclamation.setSmartPriorityLevel(result.getLevel());
                reclamation.setSmartPriorityReason(result.getReason());
                reclamationRepository.save(reclamation);
                updatedCount++;
            }

            EscalationResult escalationResult = escalationService.evaluate(reclamation);
            if (escalationResult.isEscalationRequired()) {
                escalationCandidateCount++;
            }
        }

        log.info(
                "Scheduled fixedDelay - active reclamations checked: {}, smart scores updated: {}, escalation candidates: {}",
                activeReclamations.size(),
                updatedCount,
                escalationCandidateCount
        );
    }

    @Scheduled(
            cron = "${tinyspring.scheduler.daily-summary-cron:0 0 8 * * *}",
            zone = "${tinyspring.scheduler.zone:Africa/Tunis}"
    )
    @Transactional(readOnly = true)
    public void logDailyReclamationSummary() {
        int openCount = (int) reclamationRepository.countByStatus(ReclamationStatus.OPEN);
        int inProgressCount = (int) reclamationRepository.countByStatus(ReclamationStatus.IN_PROGRESS);
        int resolvedCount = (int) reclamationRepository.countByStatus(ReclamationStatus.RESOLVED);
        int rejectedCount = (int) reclamationRepository.countByStatus(ReclamationStatus.REJECTED);
        long unassignedActiveCount = reclamationRepository.countUnassignedActive(ACTIVE_STATUSES);
        long recurringActiveCount = reclamationRepository.countRecurringActive(ACTIVE_STATUSES);

        log.info(
                "Scheduled cron - daily reclamation summary: OPEN={}, IN_PROGRESS={}, RESOLVED={}, REJECTED={}, unassignedActive={}, recurringActive={}",
                openCount,
                inProgressCount,
                resolvedCount,
                rejectedCount,
                unassignedActiveCount,
                recurringActiveCount
        );
    }

    private boolean isSmartPriorityChanged(Reclamation reclamation, SmartPriorityResult result) {
        return !Objects.equals(reclamation.getSmartPriorityScore(), result.getTotalScore())
                || !Objects.equals(reclamation.getSmartPriorityLevel(), result.getLevel());
    }
}
