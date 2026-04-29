package com.tinyspring.garderie.service.RH;

import com.tinyspring.garderie.dto.RH.DashboardStatsDTO;
import com.tinyspring.garderie.entity.RH.AbsenceConge;
import com.tinyspring.garderie.entity.RH.Animatrice;
import com.tinyspring.garderie.entity.RH.enums.StatutAbsenceConge;
import com.tinyspring.garderie.entity.RH.enums.StatutAnimatrice;
import com.tinyspring.garderie.entity.RH.enums.TypeAbsenceConge;
import com.tinyspring.garderie.repository.RH.AbsenceCongeRepository;
import com.tinyspring.garderie.repository.RH.AnimatriceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests DatasetGeneratorServiceImpl")
class DatasetGeneratorServiceImplTest {

    @TempDir
    Path tempDir;

    @Mock private AbsenceCongeRepository absenceCongeRepository;
    @Mock private AnimatriceRepository animatriceRepository;
    @Mock private IDashboardService dashboardService;

    @InjectMocks private DatasetGeneratorServiceImpl service;

    private DashboardStatsDTO stats;
    private Animatrice animatrice;
    private AbsenceConge absence;

    @BeforeEach
    void setUp() {
        stats = DashboardStatsDTO.builder()
                .totalAnimatrices(3L).animatricesActives(2L).animatricesInactives(1L)
                .totalAbsences(5L).absencesEnAttente(2L)
                .absencesApprouvees(2L).absencesRefusees(1L)
                .absences(1L).congesAnnuels(2L).congesMaladie(1L).congesMaternite(1L)
                .totalFormations(4L).formationsInscrites(2L)
                .formationsEnCours(1L).formationsTerminees(1L)
                .dernieresDemandesEnAttente(List.of())
                .dernieresAnimatrices(List.of())
                .build();

        animatrice = new Animatrice();
        animatrice.setId(1L); animatrice.setNom("Ben Ali"); animatrice.setPrenom("Sara");
        animatrice.setStatut(StatutAnimatrice.ACTIVE);
        animatrice.setSpecialite("premiers secours");

        absence = AbsenceConge.builder()
                .animatrice(animatrice)
                .type(TypeAbsenceConge.CONGE_ANNUEL)
                .dateDebut(LocalDate.now())
                .dateFin(LocalDate.now().plusDays(5))
                .statut(StatutAbsenceConge.APPROUVE)
                .nbJours(5).build();
        absence.setId(1L);
    }

    // ===== genererDataset — liste vide =====

    @Test
    @DisplayName("genererDataset — doit générer le dataset avec données vides")
    void genererDataset_doitGenererAvecDonneesVides() throws Exception {
        // Change le répertoire de sortie vers temp
        System.setProperty("user.dir", tempDir.toString());

        when(animatriceRepository.findAll()).thenReturn(List.of());
        when(absenceCongeRepository.findAll()).thenReturn(List.of());
        when(dashboardService.getStats()).thenReturn(stats);

        String result = service.genererDataset();

        assertThat(result).contains("Dataset généré");
        assertThat(result).contains("exemples");
    }

    // ===== genererDataset — avec données =====

    @Test
    @DisplayName("genererDataset — doit générer le dataset avec animatrices et absences")
    void genererDataset_doitGenererAvecDonnees() throws Exception {
        System.setProperty("user.dir", tempDir.toString());

        when(animatriceRepository.findAll()).thenReturn(List.of(animatrice));
        when(absenceCongeRepository.findAll()).thenReturn(List.of(absence));
        when(dashboardService.getStats()).thenReturn(stats);
        when(absenceCongeRepository.findByAnimatriceId(1L)).thenReturn(List.of(absence));

        String result = service.genererDataset();

        assertThat(result).contains("Dataset généré");
        assertThat(result).contains("exemples");
    }

    // ===== genererDataset — avec quotas dépassés =====

    @Test
    @DisplayName("genererDataset — doit détecter animatrice avec quota proche")
    void genererDataset_doitDetecterQuotaDepasse() throws Exception {
        System.setProperty("user.dir", tempDir.toString());

        // Animatrice avec 27 jours de congé annuel
        AbsenceConge beaucoupDeConges = AbsenceConge.builder()
                .animatrice(animatrice)
                .type(TypeAbsenceConge.CONGE_ANNUEL)
                .dateDebut(LocalDate.of(LocalDate.now().getYear(), 1, 1))
                .dateFin(LocalDate.of(LocalDate.now().getYear(), 1, 27))
                .statut(StatutAbsenceConge.APPROUVE)
                .nbJours(27).build();

        when(animatriceRepository.findAll()).thenReturn(List.of(animatrice));
        when(absenceCongeRepository.findAll()).thenReturn(List.of(beaucoupDeConges));
        when(dashboardService.getStats()).thenReturn(stats);
        when(absenceCongeRepository.findByAnimatriceId(1L)).thenReturn(List.of(beaucoupDeConges));

        String result = service.genererDataset();

        assertThat(result).contains("Dataset généré");
    }

    // ===== genererDataset — demandes en attente =====

    @Test
    @DisplayName("genererDataset — doit générer exemples demandes en attente")
    void genererDataset_doitGenererExemplesEnAttente() throws Exception {
        System.setProperty("user.dir", tempDir.toString());

        AbsenceConge enAttente = AbsenceConge.builder()
                .animatrice(animatrice)
                .type(TypeAbsenceConge.ABSENCE)
                .dateDebut(LocalDate.now().plusDays(5))
                .dateFin(LocalDate.now().plusDays(6))
                .statut(StatutAbsenceConge.EN_ATTENTE)
                .nbJours(1).build();

        DashboardStatsDTO statsAvecAttente = DashboardStatsDTO.builder()
                .totalAnimatrices(1L).animatricesActives(1L).animatricesInactives(0L)
                .totalAbsences(1L).absencesEnAttente(1L)
                .absencesApprouvees(0L).absencesRefusees(0L)
                .absences(1L).congesAnnuels(0L).congesMaladie(0L).congesMaternite(0L)
                .totalFormations(0L).formationsInscrites(0L)
                .formationsEnCours(0L).formationsTerminees(0L)
                .dernieresDemandesEnAttente(List.of())
                .dernieresAnimatrices(List.of()).build();

        when(animatriceRepository.findAll()).thenReturn(List.of(animatrice));
        when(absenceCongeRepository.findAll()).thenReturn(List.of(enAttente));
        when(dashboardService.getStats()).thenReturn(statsAvecAttente);
        when(absenceCongeRepository.findByAnimatriceId(1L)).thenReturn(List.of(enAttente));

        String result = service.genererDataset();

        assertThat(result).contains("Dataset généré");
    }

    // ===== genererDataset — animatrice sans spécialité =====

    @Test
    @DisplayName("genererDataset — doit gérer animatrice sans spécialité")
    void genererDataset_doitGererAnimatriceSansSpecialite() throws Exception {
        System.setProperty("user.dir", tempDir.toString());

        animatrice.setSpecialite(null); // pas de spécialité

        when(animatriceRepository.findAll()).thenReturn(List.of(animatrice));
        when(absenceCongeRepository.findAll()).thenReturn(List.of());
        when(dashboardService.getStats()).thenReturn(stats);
        when(absenceCongeRepository.findByAnimatriceId(1L)).thenReturn(List.of());

        String result = service.genererDataset();

        assertThat(result).contains("Dataset généré");
    }
}
