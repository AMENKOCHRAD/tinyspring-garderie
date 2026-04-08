package com.tinyspring.garderie.service.RH;

import com.tinyspring.garderie.dto.RH.AbsenceCongeDTO;
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
        AbsenceCongeDTO result = toDTO(absenceCongeRepository.save(absenceConge));

        // ✅ Email à l'animatrice
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
        AbsenceCongeDTO result = toDTO(absenceCongeRepository.save(absenceConge));

        // ✅ Email à l'animatrice
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

        AbsenceConge absenceConge = toEntity(dto, animatrice);
        absenceConge.setStatut(StatutAbsenceConge.EN_ATTENTE);
        absenceConge.setNbJours((int) ChronoUnit.DAYS.between(dto.getDateDebut(), dto.getDateFin()) + 1);
        AbsenceCongeDTO result = toDTO(absenceCongeRepository.save(absenceConge));

        // ✅ Notification temps réel à l'admin
        notificationService.creerNotification(
                "🔔 Nouvelle demande de " + animatrice.getPrenom() + " " + animatrice.getNom()
                        + " — " + dto.getType().name().replace("_", " "),
                "ABSENCE"
        );

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