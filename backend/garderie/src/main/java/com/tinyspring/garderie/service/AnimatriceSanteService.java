package com.tinyspring.garderie.service;

import com.tinyspring.garderie.dto.ObservationCreateDto;
import com.tinyspring.garderie.dto.PriseTraitementCreateDto;
import com.tinyspring.garderie.entity.*;
import com.tinyspring.garderie.repository.EnfantRepository;
import com.tinyspring.garderie.repository.ObservationEnfantRepository;
import com.tinyspring.garderie.repository.PriseTraitementRepository;
import com.tinyspring.garderie.repository.TraitementRepository;
import com.tinyspring.garderie.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class AnimatriceSanteService {

    private final TraitementRepository traitementRepository;
    private final EnfantRepository enfantRepository;
    private final PriseTraitementRepository priseTraitementRepository;
    private final ObservationEnfantRepository observationEnfantRepository;
    private final UserRepository userRepository;

    public AnimatriceSanteService(TraitementRepository traitementRepository,
                                  EnfantRepository enfantRepository,
                                  PriseTraitementRepository priseTraitementRepository,
                                  ObservationEnfantRepository observationEnfantRepository,
                                  UserRepository userRepository) {
        this.traitementRepository = traitementRepository;
        this.enfantRepository = enfantRepository;
        this.priseTraitementRepository = priseTraitementRepository;
        this.observationEnfantRepository = observationEnfantRepository;
        this.userRepository = userRepository;
    }

    public PriseTraitement enregistrerPrise(String emailAnimatrice, Long traitementId, PriseTraitementCreateDto payload) {
        User animatrice = requireUser(emailAnimatrice);
        Traitement traitement = traitementRepository.findById(traitementId)
                .orElseThrow(() -> new RuntimeException("Traitement introuvable"));

        if (payload.getDatePrise() == null) {
            payload.setDatePrise(LocalDate.now());
        }

        String heure = payload.getHeurePrevue();
        if (heure == null || heure.isBlank()) {
            throw new RuntimeException("Heure prevue obligatoire.");
        }

        if (traitement.getHeuresPrises() == null || !traitement.getHeuresPrises().contains(heure)) {
            throw new RuntimeException("Heure prevue invalide pour ce traitement.");
        }

        priseTraitementRepository.findByTraitementIdAndDatePriseAndHeurePrevue(traitementId, payload.getDatePrise(), heure)
                .ifPresent(existing -> {
                    throw new RuntimeException("Prise deja enregistree pour cette heure.");
                });

        PriseTraitement prise = new PriseTraitement();
        prise.setTraitement(traitement);
        prise.setDatePrise(payload.getDatePrise());
        prise.setHeurePrevue(heure);
        prise.setDonnePar(animatrice);
        prise.setNote(payload.getNote());

        return priseTraitementRepository.save(prise);
    }

    public List<PriseTraitement> listerPrisesParEnfant(Long enfantId, LocalDate date) {
        LocalDate effectiveDate = date != null ? date : LocalDate.now();
        return priseTraitementRepository.findByTraitementConditionSanitaireEnfantIdAndDatePrise(enfantId, effectiveDate);
    }

    public ObservationEnfant creerObservation(String emailAnimatrice, Long enfantId, ObservationCreateDto payload) {
        User animatrice = requireUser(emailAnimatrice);
        Enfant enfant = enfantRepository.findById(enfantId)
                .orElseThrow(() -> new RuntimeException("Enfant introuvable"));

        if (payload.getType() == null) {
            throw new RuntimeException("Type d'observation obligatoire.");
        }

        String titre = payload.getTitre() != null ? payload.getTitre().trim() : "";
        String description = payload.getDescription() != null ? payload.getDescription().trim() : "";
        if (titre.isBlank()) {
            throw new RuntimeException("Titre obligatoire.");
        }
        if (description.isBlank()) {
            throw new RuntimeException("Description obligatoire.");
        }

        ObservationEnfant observation = new ObservationEnfant();
        observation.setEnfant(enfant);
        observation.setType(payload.getType());
        observation.setTitre(titre);
        observation.setDescription(description);
        observation.setCreePar(animatrice);

        return observationEnfantRepository.save(observation);
    }

    public List<ObservationEnfant> listerObservationsEnfant(Long enfantId) {
        return observationEnfantRepository.findTop50ByEnfantIdOrderByCreeLeDesc(enfantId);
    }

    public List<ObservationEnfant> listerDernieresObservations() {
        return observationEnfantRepository.findTop50ByOrderByCreeLeDesc();
    }

    private User requireUser(String email) {
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Utilisateur non connecte.");
        }
        return userRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable."));
    }
}

