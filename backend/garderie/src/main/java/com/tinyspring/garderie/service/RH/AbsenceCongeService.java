package com.tinyspring.garderie.service.RH;

import com.tinyspring.garderie.dto.RH.AbsenceCongeDTO;
import com.tinyspring.garderie.dto.RH.ResultatEvaluationDTO;
import com.tinyspring.garderie.entity.RH.AbsenceConge;
import com.tinyspring.garderie.entity.RH.Animatrice;
import com.tinyspring.garderie.entity.RH.enums.StatutAbsenceConge;
import com.tinyspring.garderie.repository.RH.AbsenceCongeRepository;
import com.tinyspring.garderie.repository.RH.AnimatriceRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AbsenceCongeService {

    private final AbsenceCongeRepository absenceCongeRepository;
    private final AnimatriceRepository animatriceRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final MoteurReglesService moteurReglesService; // ✅ NOUVEAU

    // ========== ADMIN ==========

    public List<AbsenceCongeDTO> getAllAbsenceConges() {
        return absenceCongeRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<AbsenceCongeDTO> getAbsenceCongesByStatut(StatutAbsenceConge statut) {
        return absenceCongeRepository.findByStatut(statut)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public AbsenceCongeDTO validerDemande(Long id) {
        AbsenceConge absenceConge = absenceCongeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Demande non trouvée : " + id));
        absenceConge.setStatut(StatutAbsenceConge.APPROUVE);
        absenceConge.setDecisionAutomatique(false);
        absenceConge.setMotifDecision("✅ Validée manuellement par l'administrateur.");
        AbsenceCongeDTO result = toDTO(absenceCongeRepository.save(absenceConge));

        emailService.envoyerDecisionAbsence(
                absenceConge.getAnimatrice().getEmail(),
                absenceConge.getAnimatrice().getPrenom(),
                absenceConge.getAnimatrice().getNom(),
                absenceConge.getType().name(),
                absenceConge.getDateDebut().toString(),
                absenceConge.getDateFin().toString(),
                true,
                null
        );

        return result;
    }

    public AbsenceCongeDTO refuserDemande(Long id) {
        AbsenceConge absenceConge = absenceCongeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Demande non trouvée : " + id));
        absenceConge.setStatut(StatutAbsenceConge.REFUSE);
        absenceConge.setDecisionAutomatique(false);
        absenceConge.setMotifDecision("❌ Refusée manuellement par l'administrateur.");
        AbsenceCongeDTO result = toDTO(absenceCongeRepository.save(absenceConge));

        emailService.envoyerDecisionAbsence(
                absenceConge.getAnimatrice().getEmail(),
                absenceConge.getAnimatrice().getPrenom(),
                absenceConge.getAnimatrice().getNom(),
                absenceConge.getType().name(),
                absenceConge.getDateDebut().toString(),
                absenceConge.getDateFin().toString(),
                false,
                null
        );

        return result;
    }

    public void deleteAbsenceConge(Long id) {
        if (!absenceCongeRepository.existsById(id)) {
            throw new EntityNotFoundException("Demande non trouvée : " + id);
        }
        absenceCongeRepository.deleteById(id);
    }

    // ========== ANIMATRICE ==========

    public AbsenceCongeDTO soumettreDemandeAbsenceConge(AbsenceCongeDTO dto) {
        if (dto.getDateFin().isBefore(dto.getDateDebut())) {
            throw new RuntimeException("La date de fin doit être après la date de début");
        }

        Animatrice animatrice = animatriceRepository.findById(dto.getAnimatriceId())
                .orElseThrow(() -> new EntityNotFoundException("Animatrice non trouvée"));

        // 1. Créer et sauvegarder la demande en EN_ATTENTE d'abord
        AbsenceConge absenceConge = toEntity(dto, animatrice);
        absenceConge.setStatut(StatutAbsenceConge.EN_ATTENTE);
        absenceConge.setNbJours(
                (int) ChronoUnit.DAYS.between(dto.getDateDebut(), dto.getDateFin()) + 1
        );
        AbsenceConge saved = absenceCongeRepository.save(absenceConge);

        // ✅ 2. Évaluation par le moteur de règles
        ResultatEvaluationDTO resultat = moteurReglesService.evaluer(saved);

        // 3. Appliquer la décision automatique
        switch (resultat.getDecision()) {
            case "AUTO_APPROUVE" -> {
                saved.setStatut(StatutAbsenceConge.APPROUVE);
                saved.setDecisionAutomatique(true);
                saved.setMotifDecision(resultat.getExplication());
                absenceCongeRepository.save(saved);

                // Email d'approbation automatique
                emailService.envoyerDecisionAbsence(
                        animatrice.getEmail(),
                        animatrice.getPrenom(),
                        animatrice.getNom(),
                        dto.getType().name(),
                        dto.getDateDebut().toString(),
                        dto.getDateFin().toString(),
                        true,
                        null
                );

                notificationService.creerNotification(
                        "✅ Demande de " + animatrice.getPrenom() + " " + animatrice.getNom()
                                + " auto-approuvée par le moteur de règles.",
                        "ABSENCE"
                );
            }
            case "AUTO_REFUSE" -> {
                saved.setStatut(StatutAbsenceConge.REFUSE);
                saved.setDecisionAutomatique(true);
                saved.setMotifDecision(resultat.getExplication());
                absenceCongeRepository.save(saved);

                // Email de refus automatique
                emailService.envoyerDecisionAbsence(
                        animatrice.getEmail(),
                        animatrice.getPrenom(),
                        animatrice.getNom(),
                        dto.getType().name(),
                        dto.getDateDebut().toString(),
                        dto.getDateFin().toString(),
                        false,
                        resultat.getExplication()
                );

                notificationService.creerNotification(
                        "❌ Demande de " + animatrice.getPrenom() + " " + animatrice.getNom()
                                + " auto-refusée : " + resultat.getRegleDeclenchee(),
                        "ABSENCE"
                );
            }
            default -> {
                // TRANSMIS_ADMIN — reste EN_ATTENTE
                saved.setMotifDecision(resultat.getExplication());
                absenceCongeRepository.save(saved);

                notificationService.creerNotification(
                        "🔔 Nouvelle demande de " + animatrice.getPrenom() + " " + animatrice.getNom()
                                + " — " + dto.getType().name().replace("_", " ")
                                + " (validation manuelle requise)",
                        "ABSENCE"
                );
            }
        }

        // 4. Retourner le DTO avec le résultat
        AbsenceCongeDTO result = toDTO(absenceCongeRepository.findById(saved.getId()).orElse(saved));
        result.setResultatEvaluation(resultat);
        return result;
    }

    public List<AbsenceCongeDTO> getMesAbsenceConges(Long animatriceId) {
        return absenceCongeRepository.findByAnimatriceId(animatriceId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ========== MAPPING ==========

    public AbsenceCongeDTO toDTO(AbsenceConge absenceConge) {
        return AbsenceCongeDTO.builder()
                .id(absenceConge.getId())
                .animatriceId(absenceConge.getAnimatrice().getId())
                .animatriceNom(absenceConge.getAnimatrice().getNom())
                .animatricePrenom(absenceConge.getAnimatrice().getPrenom())
                .type(absenceConge.getType())
                .dateDebut(absenceConge.getDateDebut())
                .dateFin(absenceConge.getDateFin())
                .motif(absenceConge.getMotif())
                .statut(absenceConge.getStatut())
                .nbJours(absenceConge.getNbJours())
                .decisionAutomatique(Boolean.TRUE.equals(absenceConge.getDecisionAutomatique()))
                .motifDecision(absenceConge.getMotifDecision())
                .build();
    }

    private AbsenceConge toEntity(AbsenceCongeDTO dto, Animatrice animatrice) {
        return AbsenceConge.builder()
                .animatrice(animatrice)
                .type(dto.getType())
                .dateDebut(dto.getDateDebut())
                .dateFin(dto.getDateFin())
                .motif(dto.getMotif())
                .build();
    }
}