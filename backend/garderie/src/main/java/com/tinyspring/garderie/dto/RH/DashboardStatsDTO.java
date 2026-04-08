package com.tinyspring.garderie.dto.RH;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsDTO {

    // Animatrices
    private long totalAnimatrices;
    private long animatricesActives;
    private long animatricesInactives;

    // Absences
    private long totalAbsences;
    private long absencesEnAttente;
    private long absencesApprouvees;
    private long absencesRefusees;

    // Par type d'absence
    private long absences;
    private long congesAnnuels;
    private long congesMaladie;
    private long congesMaternite;

    // Formations
    private long totalFormations;
    private long formationsInscrites;
    private long formationsEnCours;
    private long formationsTerminees;

    // Listes
    private List<AbsenceCongeDTO> dernieresDemandesEnAttente;
    private List<AnimatriceDTO> dernieresAnimatrices;
}