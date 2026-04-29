package com.tinyspring.garderie.service.RH;

import com.tinyspring.garderie.entity.RH.Formation;
import com.tinyspring.garderie.entity.RH.enums.StatutFormation;
import com.tinyspring.garderie.entity.RH.enums.TypeFormation;
import com.tinyspring.garderie.repository.RH.FormationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests FormationScheduler")
class FormationSchedulerTest {

    @Mock private FormationRepository formationRepository;
    @Mock private IFormationService formationService;
    @InjectMocks private FormationScheduler scheduler;

    private Formation formationOuverte;
    private Formation formationEnCours;

    @BeforeEach
    void setUp() {
        formationOuverte = new Formation();
        formationOuverte.setId(1L);
        formationOuverte.setTitre("Secourisme");
        formationOuverte.setType(TypeFormation.SECOURISME);
        formationOuverte.setStatut(StatutFormation.OUVERTE);
        formationOuverte.setInscriptions(List.of());

        formationEnCours = new Formation();
        formationEnCours.setId(2L);
        formationEnCours.setTitre("Pédagogie");
        formationEnCours.setType(TypeFormation.PEDAGOGIE);
        formationEnCours.setStatut(StatutFormation.EN_COURS);
        formationEnCours.setInscriptions(List.of());
    }

    // ===== Aucune formation à traiter =====

    @Test
    @DisplayName("gererCycleVieAutomatique — doit ne rien faire si aucune formation")
    void scheduler_doitNeRienFaireSiAucuneFormation() {
        when(formationRepository.findByStatut(StatutFormation.OUVERTE)).thenReturn(List.of());
        when(formationRepository.findByStatut(StatutFormation.EN_COURS)).thenReturn(List.of());

        assertThatCode(() -> scheduler.gererCycleVieAutomatique())
                .doesNotThrowAnyException();

        verify(formationService, never()).demarrerAutomatique(any());
        verify(formationService, never()).terminerFormation(any());
    }

    // ===== Auto-démarrage =====

    @Test
    @DisplayName("gererCycleVieAutomatique — doit démarrer formation si heure passée aujourd'hui")
    void scheduler_doitDemarrerFormation() {
        // Formation avec heure de début passée → doit être démarrée
        formationOuverte.setDateFormation(LocalDate.now());
        formationOuverte.setHeureDebut(LocalTime.now().minusHours(1)); // 1h avant maintenant

        when(formationRepository.findByStatut(StatutFormation.OUVERTE))
                .thenReturn(List.of(formationOuverte));
        when(formationRepository.findByStatut(StatutFormation.EN_COURS))
                .thenReturn(List.of());
        when(formationService.demarrerAutomatique(1L)).thenReturn(formationOuverte);

        scheduler.gererCycleVieAutomatique();

        verify(formationService).demarrerAutomatique(1L);
    }

    @Test
    @DisplayName("gererCycleVieAutomatique — ne doit pas démarrer si heure pas encore atteinte")
    void scheduler_doitPasDemarrerSiHeurePasAtteinte() {
        formationOuverte.setDateFormation(LocalDate.now());
        formationOuverte.setHeureDebut(LocalTime.now().plusHours(2)); // dans 2h

        when(formationRepository.findByStatut(StatutFormation.OUVERTE))
                .thenReturn(List.of(formationOuverte));
        when(formationRepository.findByStatut(StatutFormation.EN_COURS))
                .thenReturn(List.of());

        scheduler.gererCycleVieAutomatique();

        verify(formationService, never()).demarrerAutomatique(any());
    }

    @Test
    @DisplayName("gererCycleVieAutomatique — ne doit pas démarrer si dateFormation null")
    void scheduler_doitPasDemarrerSiDateNull() {
        formationOuverte.setDateFormation(null); // pas de date
        formationOuverte.setHeureDebut(LocalTime.now().minusHours(1));

        when(formationRepository.findByStatut(StatutFormation.OUVERTE))
                .thenReturn(List.of(formationOuverte));
        when(formationRepository.findByStatut(StatutFormation.EN_COURS))
                .thenReturn(List.of());

        scheduler.gererCycleVieAutomatique();

        verify(formationService, never()).demarrerAutomatique(any());
    }

    // ===== Auto-terminaison =====

    @Test
    @DisplayName("gererCycleVieAutomatique — doit terminer formation si heure fin passée")
    void scheduler_doitTerminerFormation() {
        formationEnCours.setDateFormation(LocalDate.now());
        formationEnCours.setHeureFin(LocalTime.now().minusMinutes(30)); // finie il y a 30min

        when(formationRepository.findByStatut(StatutFormation.OUVERTE)).thenReturn(List.of());
        when(formationRepository.findByStatut(StatutFormation.EN_COURS))
                .thenReturn(List.of(formationEnCours));
        when(formationService.terminerFormation(2L)).thenReturn(formationEnCours);

        scheduler.gererCycleVieAutomatique();

        verify(formationService).terminerFormation(2L);
    }

    @Test
    @DisplayName("gererCycleVieAutomatique — ne doit pas terminer si heure fin pas atteinte")
    void scheduler_doitPasTerminerSiHeurePasAtteinte() {
        formationEnCours.setDateFormation(LocalDate.now());
        formationEnCours.setHeureFin(LocalTime.now().plusHours(1)); // dans 1h

        when(formationRepository.findByStatut(StatutFormation.OUVERTE)).thenReturn(List.of());
        when(formationRepository.findByStatut(StatutFormation.EN_COURS))
                .thenReturn(List.of(formationEnCours));

        scheduler.gererCycleVieAutomatique();

        verify(formationService, never()).terminerFormation(any());
    }

    // ===== Formations passées =====

    @Test
    @DisplayName("gererCycleVieAutomatique — doit terminer formation d'une date passée")
    void scheduler_doitTerminerFormationPassee() {
        formationEnCours.setDateFormation(LocalDate.now().minusDays(1)); // hier
        formationEnCours.setHeureFin(null); // pas d'heure fin → ne sera pas traité dans le bloc heure

        when(formationRepository.findByStatut(StatutFormation.OUVERTE)).thenReturn(List.of());
        when(formationRepository.findByStatut(StatutFormation.EN_COURS))
                .thenReturn(List.of(formationEnCours));
        when(formationService.terminerFormation(2L)).thenReturn(formationEnCours);

        scheduler.gererCycleVieAutomatique();

        verify(formationService).terminerFormation(2L);
    }

    // ===== Gestion des exceptions =====

    @Test
    @DisplayName("gererCycleVieAutomatique — doit continuer si exception lors du démarrage")
    void scheduler_doitContinuerSiExceptionDemarrage() {
        formationOuverte.setDateFormation(LocalDate.now());
        formationOuverte.setHeureDebut(LocalTime.now().minusHours(1));

        when(formationRepository.findByStatut(StatutFormation.OUVERTE))
                .thenReturn(List.of(formationOuverte));
        when(formationRepository.findByStatut(StatutFormation.EN_COURS))
                .thenReturn(List.of());
        when(formationService.demarrerAutomatique(1L))
                .thenThrow(new RuntimeException("Erreur démarrage"));

        // Ne doit pas propager l'exception
        assertThatCode(() -> scheduler.gererCycleVieAutomatique())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("gererCycleVieAutomatique — doit continuer si exception lors de la terminaison")
    void scheduler_doitContinuerSiExceptionTerminaison() {
        formationEnCours.setDateFormation(LocalDate.now().minusDays(1));

        when(formationRepository.findByStatut(StatutFormation.OUVERTE)).thenReturn(List.of());
        when(formationRepository.findByStatut(StatutFormation.EN_COURS))
                .thenReturn(List.of(formationEnCours));
        when(formationService.terminerFormation(2L))
                .thenThrow(new RuntimeException("Erreur terminaison"));

        assertThatCode(() -> scheduler.gererCycleVieAutomatique())
                .doesNotThrowAnyException();
    }
}
