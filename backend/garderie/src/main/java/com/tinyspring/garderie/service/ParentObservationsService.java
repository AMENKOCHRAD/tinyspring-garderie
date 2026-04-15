package com.tinyspring.garderie.service;

import com.tinyspring.garderie.entity.Enfant;
import com.tinyspring.garderie.entity.ObservationEnfant;
import com.tinyspring.garderie.repository.EnfantRepository;
import com.tinyspring.garderie.repository.ObservationEnfantRepository;
import com.tinyspring.garderie.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ParentObservationsService {

    private final UserRepository userRepository;
    private final EnfantRepository enfantRepository;
    private final ObservationEnfantRepository observationEnfantRepository;

    public ParentObservationsService(UserRepository userRepository,
                                    EnfantRepository enfantRepository,
                                    ObservationEnfantRepository observationEnfantRepository) {
        this.userRepository = userRepository;
        this.enfantRepository = enfantRepository;
        this.observationEnfantRepository = observationEnfantRepository;
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
}

