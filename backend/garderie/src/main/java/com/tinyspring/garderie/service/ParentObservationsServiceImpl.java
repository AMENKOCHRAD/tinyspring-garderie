package com.tinyspring.garderie.service;

import com.tinyspring.garderie.entity.Enfant;
import com.tinyspring.garderie.entity.ObservationEnfant;
import com.tinyspring.garderie.repository.EnfantRepository;
import com.tinyspring.garderie.repository.ObservationEnfantRepository;
import com.tinyspring.garderie.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ParentObservationsServiceImpl implements ParentObservationsService {

    private final UserRepository userRepository;
    private final EnfantRepository enfantRepository;
    private final ObservationEnfantRepository observationEnfantRepository;
    private final RealtimeNotificationService realtimeNotificationService;

    public ParentObservationsServiceImpl(UserRepository userRepository,
                                    EnfantRepository enfantRepository,
                                    ObservationEnfantRepository observationEnfantRepository,
                                    RealtimeNotificationService realtimeNotificationService) {
        this.userRepository = userRepository;
        this.enfantRepository = enfantRepository;
        this.observationEnfantRepository = observationEnfantRepository;
        this.realtimeNotificationService = realtimeNotificationService;
    }

    public List<ObservationEnfant> listerObservationsPourParent(String emailParent, Long enfantId) {
        if (emailParent == null || emailParent.isBlank()) {
            throw new RuntimeException("Utilisateur non connecte.");
        }

        userRepository.findByEmailIgnoreCase(emailParent.trim())
                .orElseThrow(() -> new RuntimeException("Parent introuvable."));

        Enfant enfant = enfantRepository.findById(enfantId)
                .orElseThrow(() -> new RuntimeException("Enfant introuvable."));

        if (enfant.getParent() == null || enfant.getParent().getEmail() == null
                || !enfant.getParent().getEmail().equalsIgnoreCase(emailParent.trim())) {
            throw new RuntimeException("Acces interdit.");
        }

        return observationEnfantRepository.findTop50ByEnfantIdOrderByCreeLeDesc(enfantId);
    }

    public List<ObservationEnfant> listerObservationsParent(String emailParent, boolean unreadOnly) {
        if (emailParent == null || emailParent.isBlank()) {
            throw new RuntimeException("Utilisateur non connecte.");
        }

        userRepository.findByEmailIgnoreCase(emailParent.trim())
                .orElseThrow(() -> new RuntimeException("Parent introuvable."));

        if (unreadOnly) {
            return observationEnfantRepository.findTop200ByEnfantParentEmailIgnoreCaseAndLuParentFalseOrderByCreeLeDesc(emailParent.trim());
        }

        return observationEnfantRepository.findTop200ByEnfantParentEmailIgnoreCaseOrderByCreeLeDesc(emailParent.trim());
    }

    public long compterNonLues(String emailParent) {
        if (emailParent == null || emailParent.isBlank()) {
            throw new RuntimeException("Utilisateur non connecte.");
        }
        userRepository.findByEmailIgnoreCase(emailParent.trim())
                .orElseThrow(() -> new RuntimeException("Parent introuvable."));
        return observationEnfantRepository.countByEnfantParentEmailIgnoreCaseAndLuParentFalse(emailParent.trim());
    }

    public ObservationEnfant marquerLue(String emailParent, Long observationId) {
        if (emailParent == null || emailParent.isBlank()) {
            throw new RuntimeException("Utilisateur non connecte.");
        }

        userRepository.findByEmailIgnoreCase(emailParent.trim())
                .orElseThrow(() -> new RuntimeException("Parent introuvable."));

        ObservationEnfant observation = observationEnfantRepository.findById(observationId)
                .orElseThrow(() -> new RuntimeException("Observation introuvable."));

        Enfant enfant = observation.getEnfant();
        if (enfant == null || enfant.getParent() == null || enfant.getParent().getEmail() == null
                || !enfant.getParent().getEmail().equalsIgnoreCase(emailParent.trim())) {
            throw new RuntimeException("Acces interdit.");
        }

        if (!observation.isLuParent()) {
            observation.setLuParent(true);
            observation.setLuLe(LocalDateTime.now());
            observation = observationEnfantRepository.save(observation);
        }

        realtimeNotificationService.notifyParentUnreadCountChanged(emailParent.trim());

        return observation;
    }
}
