package com.tinyspring.garderie.service.RH;

import com.tinyspring.garderie.dto.RH.AbsenceCongeDTO;
import com.tinyspring.garderie.dto.RH.AnimatriceDTO;
import com.tinyspring.garderie.dto.RH.DashboardStatsDTO;
import com.tinyspring.garderie.entity.RH.enums.StatutAbsenceConge;
import com.tinyspring.garderie.entity.RH.enums.StatutAnimatrice;
import com.tinyspring.garderie.entity.RH.enums.StatutFormation;
import com.tinyspring.garderie.entity.RH.enums.TypeAbsenceConge;
import com.tinyspring.garderie.repository.RH.AbsenceCongeRepository;
import com.tinyspring.garderie.repository.RH.AnimatriceRepository;
import com.tinyspring.garderie.repository.RH.FormationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final AnimatriceRepository animatriceRepository;
    private final AbsenceCongeRepository absenceCongeRepository;
    private final FormationRepository formationRepository;
    private final AnimatriceService animatriceService;
    private final AbsenceCongeService absenceCongeService;

    public DashboardStatsDTO getStats() {

        // ===== Animatrices =====
        long totalAnimatrices = animatriceRepository.count();
        long animatricesActives = animatriceRepository.findByStatut(StatutAnimatrice.ACTIVE).size();
        long animatricesInactives = animatriceRepository.findByStatut(StatutAnimatrice.INACTIVE).size();

        // ===== Absences =====
        long totalAbsences = absenceCongeRepository.count();
        long absencesEnAttente = absenceCongeRepository.findByStatut(StatutAbsenceConge.EN_ATTENTE).size();
        long absencesApprouvees = absenceCongeRepository.findByStatut(StatutAbsenceConge.APPROUVE).size();
        long absencesRefusees = absenceCongeRepository.findByStatut(StatutAbsenceConge.REFUSE).size();

        // ===== Par type =====
        long absences = absenceCongeRepository.findByType(TypeAbsenceConge.ABSENCE).size();
        long congesAnnuels = absenceCongeRepository.findByType(TypeAbsenceConge.CONGE_ANNUEL).size();
        long congesMaladie = absenceCongeRepository.findByType(TypeAbsenceConge.CONGE_MALADIE).size();
        long congesMaternite = absenceCongeRepository.findByType(TypeAbsenceConge.CONGE_MATERNITE).size();

        // ===== Formations =====
        long totalFormations = formationRepository.count();
        long formationsInscrites = formationRepository.findByStatutInscription(StatutFormation.INSCRITE).size();
        long formationsEnCours = formationRepository.findByStatutInscription(StatutFormation.EN_COURS).size();
        long formationsTerminees = formationRepository.findByStatutInscription(StatutFormation.TERMINEE).size();

        // ===== Dernières demandes en attente =====
        List<AbsenceCongeDTO> dernieresDemandesEnAttente = absenceCongeRepository
                .findByStatut(StatutAbsenceConge.EN_ATTENTE)
                .stream()
                .limit(5)
                .map(absenceCongeService::toDTO)
                .collect(Collectors.toList());

        // ===== Dernières animatrices =====
        List<AnimatriceDTO> dernieresAnimatrices = animatriceRepository
                .findAll()
                .stream()
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                .limit(5)
                .map(animatriceService::toDTO)
                .collect(Collectors.toList());

        return DashboardStatsDTO.builder()
                .totalAnimatrices(totalAnimatrices)
                .animatricesActives(animatricesActives)
                .animatricesInactives(animatricesInactives)
                .totalAbsences(totalAbsences)
                .absencesEnAttente(absencesEnAttente)
                .absencesApprouvees(absencesApprouvees)
                .absencesRefusees(absencesRefusees)
                .absences(absences)
                .congesAnnuels(congesAnnuels)
                .congesMaladie(congesMaladie)
                .congesMaternite(congesMaternite)
                .totalFormations(totalFormations)
                .formationsInscrites(formationsInscrites)
                .formationsEnCours(formationsEnCours)
                .formationsTerminees(formationsTerminees)
                .dernieresDemandesEnAttente(dernieresDemandesEnAttente)
                .dernieresAnimatrices(dernieresAnimatrices)
                .build();
    }

    // Méthode publique pour toDTO absence
    public AbsenceCongeDTO toDTO(com.tinyspring.garderie.entity.RH.AbsenceConge absenceConge) {
        return absenceCongeService.toDTO(absenceConge);
    }
}