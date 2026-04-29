package com.tinyspring.garderie.service.RH;

import com.tinyspring.garderie.dto.RH.AbsenceCongeDTO;
import com.tinyspring.garderie.dto.RH.AnimatriceDTO;
import com.tinyspring.garderie.dto.RH.DashboardStatsDTO;
import com.tinyspring.garderie.dto.RH.mapper.AbsenceCongeMapper;
import com.tinyspring.garderie.dto.RH.mapper.AnimatriceMapper;
import com.tinyspring.garderie.entity.RH.AbsenceConge;
import com.tinyspring.garderie.entity.RH.Animatrice;
import com.tinyspring.garderie.entity.RH.enums.StatutAbsenceConge;
import com.tinyspring.garderie.entity.RH.enums.StatutAnimatrice;
import com.tinyspring.garderie.entity.RH.enums.StatutFormation;
import com.tinyspring.garderie.entity.RH.enums.TypeAbsenceConge;
import com.tinyspring.garderie.repository.RH.AbsenceCongeRepository;
import com.tinyspring.garderie.repository.RH.AnimatriceRepository;
import com.tinyspring.garderie.repository.RH.FormationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests DashboardServiceImpl")
class DashboardServiceImplTest {

    @Mock private AnimatriceRepository animatriceRepository;
    @Mock private AbsenceCongeRepository absenceCongeRepository;
    @Mock private FormationRepository formationRepository;
    @Mock private AnimatriceMapper animatriceMapper;
    @Mock private AbsenceCongeMapper absenceCongeMapper;

    @InjectMocks private DashboardServiceImpl service;

    private Animatrice animatrice;
    private AbsenceConge absence;

    @BeforeEach
    void setUp() {
        animatrice = new Animatrice();
        animatrice.setId(1L);
        animatrice.setNom("Ben Ali");
        animatrice.setPrenom("Sara");
        animatrice.setStatut(StatutAnimatrice.ACTIVE);

        absence = AbsenceConge.builder()
                .animatrice(animatrice)
                .type(TypeAbsenceConge.CONGE_ANNUEL)
                .dateDebut(LocalDate.now())
                .dateFin(LocalDate.now().plusDays(3))
                .statut(StatutAbsenceConge.EN_ATTENTE)
                .nbJours(3)
                .build();
        absence.setId(1L);
    }

    // ===== getStats =====
    @Test
    @DisplayName("getStats — doit retourner les statistiques complètes")
    void getStats_doitRetournerStatsCompletes() {
        // Animatrices
        when(animatriceRepository.count()).thenReturn(5L);
        when(animatriceRepository.findByStatut(StatutAnimatrice.ACTIVE))
                .thenReturn(List.of(animatrice));
        when(animatriceRepository.findByStatut(StatutAnimatrice.INACTIVE))
                .thenReturn(List.of());
        when(animatriceRepository.findAll()).thenReturn(List.of(animatrice));

        // Absences
        when(absenceCongeRepository.count()).thenReturn(10L);
        when(absenceCongeRepository.findByStatut(StatutAbsenceConge.EN_ATTENTE))
                .thenReturn(List.of(absence));
        when(absenceCongeRepository.findByStatut(StatutAbsenceConge.APPROUVE))
                .thenReturn(List.of());
        when(absenceCongeRepository.findByStatut(StatutAbsenceConge.REFUSE))
                .thenReturn(List.of());

        // Par type
        when(absenceCongeRepository.findByType(TypeAbsenceConge.ABSENCE)).thenReturn(List.of());
        when(absenceCongeRepository.findByType(TypeAbsenceConge.CONGE_ANNUEL))
                .thenReturn(List.of(absence));
        when(absenceCongeRepository.findByType(TypeAbsenceConge.CONGE_MALADIE))
                .thenReturn(List.of());
        when(absenceCongeRepository.findByType(TypeAbsenceConge.CONGE_MATERNITE))
                .thenReturn(List.of());

        // Formations
        when(formationRepository.count()).thenReturn(8L);
        when(formationRepository.countByStatut(StatutFormation.OUVERTE)).thenReturn(3L);
        when(formationRepository.countByStatut(StatutFormation.EN_COURS)).thenReturn(2L);
        when(formationRepository.countByStatut(StatutFormation.TERMINEE)).thenReturn(3L);

        // Mappers
        when(absenceCongeMapper.toDTO(any())).thenReturn(new AbsenceCongeDTO());
        when(animatriceMapper.toDTO(any())).thenReturn(new AnimatriceDTO());

        DashboardStatsDTO result = service.getStats();

        assertThat(result).isNotNull();
        assertThat(result.getTotalAnimatrices()).isEqualTo(5L);
        assertThat(result.getTotalAbsences()).isEqualTo(10L);
        assertThat(result.getTotalFormations()).isEqualTo(8L);
        assertThat(result.getAbsencesEnAttente()).isEqualTo(1L);
        assertThat(result.getCongesAnnuels()).isEqualTo(1L);
        assertThat(result.getDernieresDemandesEnAttente()).isNotNull();
        assertThat(result.getDernieresAnimatrices()).isNotNull();
    }

    @Test
    @DisplayName("getStats — doit retourner des listes vides si aucune donnée")
    void getStats_doitRetournerListesVidessSiAucuneDonnee() {
        when(animatriceRepository.count()).thenReturn(0L);
        when(animatriceRepository.findByStatut(any())).thenReturn(List.of());
        when(animatriceRepository.findAll()).thenReturn(List.of());
        when(absenceCongeRepository.count()).thenReturn(0L);
        when(absenceCongeRepository.findByStatut(any())).thenReturn(List.of());
        when(absenceCongeRepository.findByType(any())).thenReturn(List.of());
        when(formationRepository.count()).thenReturn(0L);
        when(formationRepository.countByStatut(any())).thenReturn(0L);

        DashboardStatsDTO result = service.getStats();

        assertThat(result.getTotalAnimatrices()).isEqualTo(0L);
        assertThat(result.getTotalAbsences()).isEqualTo(0L);
        assertThat(result.getDernieresDemandesEnAttente()).isEmpty();
        assertThat(result.getDernieresAnimatrices()).isEmpty();
    }
}