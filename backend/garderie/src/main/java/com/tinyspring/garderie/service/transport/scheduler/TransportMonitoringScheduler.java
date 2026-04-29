package com.tinyspring.garderie.service.transport.scheduler;

import com.tinyspring.garderie.entity.transport.DemandeTransport;
import com.tinyspring.garderie.entity.transport.StatutDemandeTransport;
import com.tinyspring.garderie.repository.transport.DemandeTransportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class TransportMonitoringScheduler {

    private static final Logger logger = LoggerFactory.getLogger(TransportMonitoringScheduler.class);

    private final DemandeTransportRepository demandeTransportRepository;

    public TransportMonitoringScheduler(DemandeTransportRepository demandeTransportRepository) {
        this.demandeTransportRepository = demandeTransportRepository;
    }

    @Scheduled(fixedRate = 300000)
    public void monitorPendingDemandes() {
        List<DemandeTransport> pendingDemandes =
                demandeTransportRepository.findDemandesByStatutOrderByOldestFirst(StatutDemandeTransport.EN_ATTENTE);

        long suspiciousPendingCount =
                demandeTransportRepository.countSuspiciousDemandesByStatut(StatutDemandeTransport.EN_ATTENTE);

        logger.info(
                "Scheduler transport: {} demandes en attente, {} suspectes.",
                pendingDemandes.size(),
                suspiciousPendingCount
        );

        if (pendingDemandes.isEmpty()) {
            return;
        }

        DemandeTransport oldestPendingDemande = pendingDemandes.get(0);
        if (oldestPendingDemande.getDateDemande() != null
                && oldestPendingDemande.getDateDemande().isBefore(LocalDate.now())) {
            logger.warn(
                    "Scheduler transport: demande en attente depuis {}. demandeId={}, enfantId={}, parentId={}",
                    oldestPendingDemande.getDateDemande(),
                    oldestPendingDemande.getId(),
                    oldestPendingDemande.getEnfant().getId(),
                    oldestPendingDemande.getParent().getId()
            );
        }
    }
}
