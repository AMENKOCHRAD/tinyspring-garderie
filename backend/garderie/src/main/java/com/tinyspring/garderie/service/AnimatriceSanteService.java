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
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AnimatriceSanteService {

    private final TraitementRepository traitementRepository;
    private final EnfantRepository enfantRepository;
    private final PriseTraitementRepository priseTraitementRepository;
    private final ObservationEnfantRepository observationEnfantRepository;
    private final UserRepository userRepository;
    private final PriseTraitementPdfService priseTraitementPdfService;

    public AnimatriceSanteService(TraitementRepository traitementRepository,
                                  EnfantRepository enfantRepository,
                                  PriseTraitementRepository priseTraitementRepository,
                                  ObservationEnfantRepository observationEnfantRepository,
                                  UserRepository userRepository,
                                  PriseTraitementPdfService priseTraitementPdfService) {
        this.traitementRepository = traitementRepository;
        this.enfantRepository = enfantRepository;
        this.priseTraitementRepository = priseTraitementRepository;
        this.observationEnfantRepository = observationEnfantRepository;
        this.userRepository = userRepository;
        this.priseTraitementPdfService = priseTraitementPdfService;
    }

    public PriseTraitement enregistrerPrise(String emailAnimatrice, Long traitementId, PriseTraitementCreateDto payload) {
        User animatrice = requireUser(emailAnimatrice);
        Traitement traitement = traitementRepository.findById(traitementId)
                .orElseThrow(() -> new RuntimeException("Traitement introuvable"));

        if (traitement.getStatut() != StatutTraitement.VALIDE && traitement.getStatut() != StatutTraitement.ACTIF) {
            throw new RuntimeException("Traitement non valide (validation admin requise).");
        }

        if (payload.getDatePrise() == null) {
            payload.setDatePrise(LocalDate.now());
        }

        if (traitement.getDateDebut() != null && payload.getDatePrise().isBefore(traitement.getDateDebut())) {
            throw new RuntimeException("Date de prise avant la date de debut du traitement.");
        }
        if (traitement.getDateFin() != null && payload.getDatePrise().isAfter(traitement.getDateFin())) {
            throw new RuntimeException("Date de prise apres la date de fin du traitement.");
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

    public List<PriseTraitement> listerPrisesParEnfantPeriode(Long enfantId, LocalDate from, LocalDate to) {
        LocalDate start = from != null ? from : LocalDate.now();
        LocalDate end = to != null ? to : start;
        if (end.isBefore(start)) {
            LocalDate tmp = start;
            start = end;
            end = tmp;
        }
        return priseTraitementRepository.findByTraitementConditionSanitaireEnfantIdAndDatePriseBetween(enfantId, start, end);
    }

    public List<PriseTraitement> listerToutesPrises(String emailAnimatrice, LocalDate date) {
        requireUser(emailAnimatrice);
        LocalDate effectiveDate = date != null ? date : LocalDate.now();
        return priseTraitementRepository.findByDatePriseOrderByHeurePrevueAscDonneLeAsc(effectiveDate);
    }

    public List<PriseTraitement> listerMesPrises(String emailAnimatrice, LocalDate from, LocalDate to) {
        User animatrice = requireUser(emailAnimatrice);
        LocalDate start = from != null ? from : LocalDate.now();
        LocalDate end = to != null ? to : start;
        if (end.isBefore(start)) {
            LocalDate tmp = start;
            start = end;
            end = tmp;
        }
        return priseTraitementRepository.findByDonneParEmailIgnoreCaseAndDatePriseBetweenOrderByDatePriseDescHeurePrevueAscDonneLeDesc(
                animatrice.getEmail(), start, end);
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
        observation.setUrgence(payload.getUrgence());
        observation.setTemperature(payload.getTemperature());
        observation.setLieu(payload.getLieu() != null ? payload.getLieu().trim() : null);
        observation.setSymptomes(payload.getSymptomes() != null ? payload.getSymptomes().trim() : null);
        observation.setActionsEffectuees(payload.getActionsEffectuees() != null ? payload.getActionsEffectuees().trim() : null);

        if (payload.getObserveLe() != null && !payload.getObserveLe().isBlank()) {
            try {
                observation.setObserveLe(LocalDateTime.parse(payload.getObserveLe().trim()));
            } catch (Exception ignore) {
                // keep null if parse fails
            }
        }

        return observationEnfantRepository.save(observation);
    }

    public byte[] genererPdfPrise(String emailAnimatrice, Long priseId) {
        requireUser(emailAnimatrice);
        PriseTraitement prise = priseTraitementRepository.findById(priseId)
                .orElseThrow(() -> new RuntimeException("Prise introuvable."));
        return priseTraitementPdfService.genererPdf(prise);
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
