package com.tinyspring.garderie.service.RH;

import com.tinyspring.garderie.dto.RH.AbsenceCongeDTO;
import com.tinyspring.garderie.dto.RH.ResultatEvaluationDTO;
import com.tinyspring.garderie.dto.RH.mapper.AbsenceCongeMapper;
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
public class AbsenceCongeServiceImpl implements IAbsenceCongeService {

    private final AbsenceCongeRepository absenceCongeRepository;
    private final AnimatriceRepository animatriceRepository;
    private final IEmailService emailService;
    private final INotificationService notificationService;
    private final IMoteurReglesService moteurReglesService;
    private final AbsenceCongeMapper absenceCongeMapper;

    // ========== ADMIN ==========

    @Override
    public List<AbsenceCongeDTO> getAllAbsenceConges() {
        return absenceCongeRepository.findAll().stream()
                .map(absenceCongeMapper::toDTO)
                .collect(Collectors.toList());
    }

    // ✅ AJOUT — Détail par ID
    @Override
    public AbsenceCongeDTO getAbsenceCongeById(Long id) {
        AbsenceConge absenceConge = absenceCongeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Demande non trouvée : " + id));
        return absenceCongeMapper.toDTO(absenceConge);
    }

    @Override
    public List<AbsenceCongeDTO> getAbsenceCongesByStatut(StatutAbsenceConge statut) {
        return absenceCongeRepository.findByStatut(statut).stream()
                .map(absenceCongeMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public AbsenceCongeDTO validerDemande(Long id) {
        AbsenceConge absenceConge = absenceCongeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Demande non trouvée : " + id));

        absenceConge.setStatut(StatutAbsenceConge.APPROUVE);
        absenceConge.setDecisionAutomatique(false);
        absenceConge.setMotifDecision("✅ Validée manuellement par l'administrateur.");

        emailService.envoyerDecisionAbsence(
                absenceConge.getAnimatrice().getEmail(),
                absenceConge.getAnimatrice().getPrenom(),
                absenceConge.getAnimatrice().getNom(),
                absenceConge.getType().name(),
                absenceConge.getDateDebut().toString(),
                absenceConge.getDateFin().toString(),
                true, null
        );

        return absenceCongeMapper.toDTO(absenceCongeRepository.save(absenceConge));
    }

    @Override
    public AbsenceCongeDTO refuserDemande(Long id) {
        AbsenceConge absenceConge = absenceCongeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Demande non trouvée : " + id));

        absenceConge.setStatut(StatutAbsenceConge.REFUSE);
        absenceConge.setDecisionAutomatique(false);
        absenceConge.setMotifDecision("❌ Refusée manuellement par l'administrateur.");

        emailService.envoyerDecisionAbsence(
                absenceConge.getAnimatrice().getEmail(),
                absenceConge.getAnimatrice().getPrenom(),
                absenceConge.getAnimatrice().getNom(),
                absenceConge.getType().name(),
                absenceConge.getDateDebut().toString(),
                absenceConge.getDateFin().toString(),
                false, null
        );

        return absenceCongeMapper.toDTO(absenceCongeRepository.save(absenceConge));
    }

    @Override
    public void deleteAbsenceConge(Long id) {
        if (!absenceCongeRepository.existsById(id))
            throw new EntityNotFoundException("Demande non trouvée : " + id);
        absenceCongeRepository.deleteById(id);
    }

    // ========== ANIMATRICE ==========

    @Override
    public AbsenceCongeDTO soumettreDemandeAbsenceConge(AbsenceCongeDTO dto) {
        if (dto.getDateFin().isBefore(dto.getDateDebut()))
            throw new RuntimeException("La date de fin doit être après la date de début");

        Animatrice animatrice = animatriceRepository.findById(dto.getAnimatriceId())
                .orElseThrow(() -> new EntityNotFoundException("Animatrice non trouvée"));

        AbsenceConge absenceConge = AbsenceConge.builder()
                .animatrice(animatrice)
                .type(dto.getType())
                .dateDebut(dto.getDateDebut())
                .dateFin(dto.getDateFin())
                .motif(dto.getMotif())
                .statut(StatutAbsenceConge.EN_ATTENTE)
                .nbJours((int) ChronoUnit.DAYS.between(dto.getDateDebut(), dto.getDateFin()) + 1)
                .build();
        AbsenceConge saved = absenceCongeRepository.save(absenceConge);

        ResultatEvaluationDTO resultat = moteurReglesService.evaluer(saved);

        switch (resultat.getDecision()) {
            case "AUTO_APPROUVE" -> {
                saved.setStatut(StatutAbsenceConge.APPROUVE);
                saved.setDecisionAutomatique(true);
                saved.setMotifDecision(resultat.getExplication());
                absenceCongeRepository.save(saved);
                emailService.envoyerDecisionAbsence(
                        animatrice.getEmail(), animatrice.getPrenom(), animatrice.getNom(),
                        dto.getType().name(), dto.getDateDebut().toString(),
                        dto.getDateFin().toString(), true, null
                );
                notificationService.creerNotification(
                        "✅ Demande de " + animatrice.getPrenom() + " " + animatrice.getNom()
                                + " auto-approuvée par le moteur de règles.", "ABSENCE"
                );
            }
            case "AUTO_REFUSE" -> {
                saved.setStatut(StatutAbsenceConge.REFUSE);
                saved.setDecisionAutomatique(true);
                saved.setMotifDecision(resultat.getExplication());
                absenceCongeRepository.save(saved);
                emailService.envoyerDecisionAbsence(
                        animatrice.getEmail(), animatrice.getPrenom(), animatrice.getNom(),
                        dto.getType().name(), dto.getDateDebut().toString(),
                        dto.getDateFin().toString(), false, resultat.getExplication()
                );
                notificationService.creerNotification(
                        "❌ Demande de " + animatrice.getPrenom() + " " + animatrice.getNom()
                                + " auto-refusée : " + resultat.getRegleDeclenchee(), "ABSENCE"
                );
            }
            default -> {
                saved.setMotifDecision(resultat.getExplication());
                absenceCongeRepository.save(saved);
                notificationService.creerNotification(
                        "🔔 Nouvelle demande de " + animatrice.getPrenom() + " " + animatrice.getNom()
                                + " — " + dto.getType().name().replace("_", " ")
                                + " (validation manuelle requise)", "ABSENCE"
                );
            }
        }

        AbsenceCongeDTO result = absenceCongeMapper.toDTO(
                absenceCongeRepository.findById(saved.getId()).orElse(saved)
        );
        result.setResultatEvaluation(resultat);
        return result;
    }

    @Override
    public List<AbsenceCongeDTO> getMesAbsenceConges(Long animatriceId) {
        return absenceCongeRepository.findByAnimatriceId(animatriceId).stream()
                .map(absenceCongeMapper::toDTO)
                .collect(Collectors.toList());
    }
}