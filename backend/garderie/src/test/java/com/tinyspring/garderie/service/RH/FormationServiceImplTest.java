package com.tinyspring.garderie.service.RH;

import com.tinyspring.garderie.dto.RH.NotificationDTO;
import com.tinyspring.garderie.entity.RH.Animatrice;
import com.tinyspring.garderie.entity.RH.AnimatriceFormation;
import com.tinyspring.garderie.entity.RH.Formation;
import com.tinyspring.garderie.entity.RH.enums.*;
import com.tinyspring.garderie.repository.RH.AnimatriceFormationRepository;
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
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests FormationServiceImpl")
class FormationServiceImplTest {

    @Mock private FormationRepository formationRepository;
    @Mock private AnimatriceFormationRepository animatriceFormationRepository;
    @Mock private AnimatriceRepository animatriceRepository;
    @Mock private INotificationService notificationService;

    @InjectMocks private FormationServiceImpl service;

    private Formation formation;
    private Animatrice animatrice;

    @BeforeEach
    void setUp() {
        animatrice = new Animatrice();
        animatrice.setId(1L);
        animatrice.setNom("Ben Ali"); animatrice.setPrenom("Sara");
        animatrice.setSpecialite("premiers secours");
        animatrice.setStatut(StatutAnimatrice.ACTIVE);

        formation = new Formation();
        formation.setId(1L);
        formation.setTitre("Premiers Secours");
        formation.setType(TypeFormation.SECOURISME);
        formation.setFormateur("Dr. Martin");
        formation.setStatut(StatutFormation.OUVERTE);
        formation.setObligatoire(false);
        formation.setPlacesMax(10);
        formation.setInscriptions(List.of());
    }

    // ===== creerFormation =====
    @Test
    @DisplayName("creerFormation — doit sauvegarder avec statut OUVERTE")
    void creerFormation_doitSauvegarder() {
        Formation nouvelle = new Formation();
        nouvelle.setTitre("Nutrition"); nouvelle.setType(TypeFormation.NUTRITION);
        nouvelle.setObligatoire(false); nouvelle.setInscriptions(List.of());

        when(formationRepository.save(any())).thenReturn(formation);
        when(animatriceRepository.findByStatut(StatutAnimatrice.ACTIVE)).thenReturn(List.of(animatrice));
        when(notificationService.creerNotification(any(), any())).thenReturn(new NotificationDTO());

        service.creerFormation(nouvelle);

        assertThat(nouvelle.getStatut()).isEqualTo(StatutFormation.OUVERTE);
        verify(formationRepository).save(nouvelle);
        verify(notificationService, atLeastOnce()).creerNotification(any(), any());
    }

    // ===== getToutesFormations =====
    @Test
    @DisplayName("getToutesFormations — doit retourner toutes les formations")
    void getToutesFormations_doitRetourner() {
        when(formationRepository.findAll()).thenReturn(List.of(formation));
        assertThat(service.getToutesFormations()).hasSize(1);
    }

    // ===== getFormationById =====
    @Test
    @DisplayName("getFormationById — doit retourner si trouvée")
    void getFormationById_doitRetourner() {
        when(formationRepository.findById(1L)).thenReturn(Optional.of(formation));
        assertThat(service.getFormationById(1L).getTitre()).isEqualTo("Premiers Secours");
    }

    @Test
    @DisplayName("getFormationById — doit lever exception si introuvable")
    void getFormationById_doitLeverException() {
        when(formationRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getFormationById(99L))
                .isInstanceOf(RuntimeException.class).hasMessageContaining("99");
    }

    // ===== modifierFormation =====
    @Test
    @DisplayName("modifierFormation — doit mettre à jour les champs")
    void modifierFormation_doitMettreAJour() {
        Formation modifications = new Formation();
        modifications.setTitre("Nouveau Titre");
        modifications.setType(TypeFormation.SANTE);
        modifications.setFormateur("Nouveau Formateur");

        when(formationRepository.findById(1L)).thenReturn(Optional.of(formation));
        when(formationRepository.save(any())).thenReturn(formation);

        service.modifierFormation(1L, modifications);

        assertThat(formation.getTitre()).isEqualTo("Nouveau Titre");
        verify(formationRepository).save(formation);
    }

    // ===== supprimerFormation =====
    @Test
    @DisplayName("supprimerFormation — doit supprimer formation et inscriptions")
    void supprimerFormation_doitSupprimer() {
        doNothing().when(animatriceFormationRepository).deleteByFormationId(1L);
        doNothing().when(formationRepository).deleteById(1L);

        service.supprimerFormation(1L);

        verify(animatriceFormationRepository).deleteByFormationId(1L);
        verify(formationRepository).deleteById(1L);
    }

    // ===== demarrerFormation =====
    @Test
    @DisplayName("demarrerFormation — doit passer en EN_COURS")
    void demarrerFormation_doitPasserEnCours() {
        AnimatriceFormation ins = new AnimatriceFormation();
        ins.setStatut(StatutInscription.INSCRITE);
        formation.setInscriptions(List.of(ins, ins));

        when(formationRepository.findById(1L)).thenReturn(Optional.of(formation));
        when(formationRepository.save(any())).thenReturn(formation);
        when(animatriceFormationRepository.findByFormationId(1L)).thenReturn(List.of());

        service.demarrerFormation(1L);

        assertThat(formation.getStatut()).isEqualTo(StatutFormation.EN_COURS);
    }

    @Test
    @DisplayName("demarrerFormation — doit lever exception si aucun inscrit")
    void demarrerFormation_doitLeverExceptionSiAucunInscrit() {
        formation.setInscriptions(List.of());
        when(formationRepository.findById(1L)).thenReturn(Optional.of(formation));
        assertThatThrownBy(() -> service.demarrerFormation(1L))
                .isInstanceOf(RuntimeException.class).hasMessageContaining("aucune animatrice inscrite");
    }

    @Test
    @DisplayName("demarrerFormation — doit lever exception si pas OUVERTE")
    void demarrerFormation_doitLeverExceptionSiPasOuverte() {
        formation.setStatut(StatutFormation.EN_COURS);
        when(formationRepository.findById(1L)).thenReturn(Optional.of(formation));
        assertThatThrownBy(() -> service.demarrerFormation(1L))
                .isInstanceOf(RuntimeException.class).hasMessageContaining("OUVERTE");
    }

    // ===== demarrerAutomatique =====
    @Test
    @DisplayName("demarrerAutomatique — doit démarrer si OUVERTE")
    void demarrerAutomatique_doitDemarrer() {
        AnimatriceFormation ins = new AnimatriceFormation();
        ins.setStatut(StatutInscription.INSCRITE);
        formation.setInscriptions(List.of(ins));

        when(formationRepository.findById(1L)).thenReturn(Optional.of(formation));
        when(formationRepository.save(any())).thenReturn(formation);
        when(animatriceFormationRepository.findByFormationId(1L)).thenReturn(List.of());

        service.demarrerAutomatique(1L);

        assertThat(formation.getStatut()).isEqualTo(StatutFormation.EN_COURS);
    }

    @Test
    @DisplayName("demarrerAutomatique — doit ignorer si pas OUVERTE")
    void demarrerAutomatique_doitIgnorerSiPasOuverte() {
        formation.setStatut(StatutFormation.EN_COURS);
        when(formationRepository.findById(1L)).thenReturn(Optional.of(formation));

        service.demarrerAutomatique(1L);

        verify(formationRepository, never()).save(any());
    }

    // ===== terminerFormation =====
    @Test
    @DisplayName("terminerFormation — doit lever exception si pas EN_COURS")
    void terminerFormation_doitLeverException() {
        when(formationRepository.findById(1L)).thenReturn(Optional.of(formation));
        assertThatThrownBy(() -> service.terminerFormation(1L))
                .isInstanceOf(RuntimeException.class).hasMessageContaining("EN_COURS");
    }

    @Test
    @DisplayName("terminerFormation — doit générer certificats pour inscrits")
    void terminerFormation_doitGenererCertificats() {
        formation.setStatut(StatutFormation.EN_COURS);
        formation.setDureeValiditeMois(12);

        AnimatriceFormation ins = new AnimatriceFormation();
        ins.setStatut(StatutInscription.INSCRITE);
        ins.setFormation(formation); ins.setAnimatrice(animatrice);

        when(formationRepository.findById(1L)).thenReturn(Optional.of(formation));
        when(formationRepository.save(any())).thenReturn(formation);
        when(animatriceFormationRepository.findByFormationId(1L)).thenReturn(List.of(ins));
        when(animatriceFormationRepository.save(any())).thenReturn(ins);
        when(notificationService.creerNotification(any(), any())).thenReturn(new NotificationDTO());

        service.terminerFormation(1L);

        assertThat(formation.getStatut()).isEqualTo(StatutFormation.TERMINEE);
        assertThat(ins.getCertificationGeneree()).isTrue();
    }

    // ===== annulerFormation =====
    @Test
    @DisplayName("annulerFormation — doit lever exception si TERMINEE")
    void annulerFormation_doitLeverException() {
        formation.setStatut(StatutFormation.TERMINEE);
        when(formationRepository.findById(1L)).thenReturn(Optional.of(formation));
        assertThatThrownBy(() -> service.annulerFormation(1L, "Motif"))
                .isInstanceOf(RuntimeException.class).hasMessageContaining("Impossible d'annuler");
    }

    @Test
    @DisplayName("annulerFormation — doit annuler et notifier")
    void annulerFormation_doitAnnuler() {
        when(formationRepository.findById(1L)).thenReturn(Optional.of(formation));
        when(formationRepository.save(any())).thenReturn(formation);
        when(animatriceFormationRepository.findByFormationId(1L)).thenReturn(List.of());

        service.annulerFormation(1L, "Motif annulation");

        assertThat(formation.getStatut()).isEqualTo(StatutFormation.ANNULEE);
    }

    // ===== inscrireAnimatrice =====
    @Test
    @DisplayName("inscrireAnimatrice — doit lever exception si pas OUVERTE")
    void inscrireAnimatrice_doitLeverExceptionSiPasOuverte() {
        formation.setStatut(StatutFormation.EN_COURS);
        when(formationRepository.findById(1L)).thenReturn(Optional.of(formation));
        when(animatriceRepository.findById(1L)).thenReturn(Optional.of(animatrice));
        assertThatThrownBy(() -> service.inscrireAnimatrice(1L, 1L))
                .isInstanceOf(RuntimeException.class).hasMessageContaining("ouverte");
    }

    @Test
    @DisplayName("inscrireAnimatrice — doit lever exception si déjà inscrite")
    void inscrireAnimatrice_doitLeverExceptionSiDejaInscrite() {
        when(formationRepository.findById(1L)).thenReturn(Optional.of(formation));
        when(animatriceRepository.findById(1L)).thenReturn(Optional.of(animatrice));
        when(animatriceFormationRepository.existsByAnimatriceIdAndFormationId(1L, 1L)).thenReturn(true);
        assertThatThrownBy(() -> service.inscrireAnimatrice(1L, 1L))
                .isInstanceOf(RuntimeException.class).hasMessageContaining("déjà inscrite");
    }

    // ===== desinscrireAnimatrice =====
    @Test
    @DisplayName("desinscrireAnimatrice — doit changer statut en ABANDONNEE")
    void desinscrireAnimatrice_doitChanger() {
        AnimatriceFormation ins = AnimatriceFormation.builder()
                .animatrice(animatrice).formation(formation)
                .statut(StatutInscription.INSCRITE)
                .dateInscription(LocalDate.now()).build();

        when(animatriceFormationRepository.findByAnimatriceIdAndFormationId(1L, 1L))
                .thenReturn(Optional.of(ins));
        when(animatriceFormationRepository.save(any())).thenReturn(ins);
        // promouvoirListeAttente appelé après
        when(formationRepository.findById(1L)).thenReturn(Optional.of(formation));
        when(animatriceFormationRepository.findByFormationIdAndStatutOrderByDateInscriptionAsc(any(), any()))
                .thenReturn(List.of());

        service.desinscrireAnimatrice(1L, 1L);

        assertThat(ins.getStatut()).isEqualTo(StatutInscription.ABANDONNEE);
        verify(animatriceFormationRepository).save(ins);
    }

    // ===== getSuggestions =====
    @Test
    @DisplayName("getSuggestions — doit retourner RECOMMANDEE pour premiers secours")
    void getSuggestions_doitRecommender() {
        when(animatriceRepository.findById(1L)).thenReturn(Optional.of(animatrice));
        when(animatriceFormationRepository.findFormationIdsSuiviesByAnimatriceId(1L)).thenReturn(List.of());
        when(animatriceFormationRepository.findFormationsExpireesByAnimatrice(1L)).thenReturn(List.of());
        when(formationRepository.findByStatut(StatutFormation.OUVERTE)).thenReturn(List.of(formation));
        when(animatriceFormationRepository.existsByAnimatriceIdAndFormationId(1L, 1L)).thenReturn(false);

        List<Map<String, Object>> suggestions = service.getSuggestions(1L);

        assertThat(suggestions).isNotEmpty();
        assertThat(suggestions.stream().anyMatch(s -> "RECOMMANDÉE".equals(s.get("priorite")))).isTrue();
    }

    // ===== getStatsFormations =====
    @Test
    @DisplayName("getStatsFormations — doit retourner les statistiques")
    void getStatsFormations_doitRetournerStats() {
        when(formationRepository.count()).thenReturn(10L);
        when(formationRepository.countByStatut(StatutFormation.OUVERTE)).thenReturn(3L);
        when(formationRepository.countByStatut(StatutFormation.EN_COURS)).thenReturn(2L);
        when(formationRepository.countByStatut(StatutFormation.TERMINEE)).thenReturn(4L);
        when(formationRepository.countByStatut(StatutFormation.ANNULEE)).thenReturn(1L);
        when(animatriceFormationRepository.findAll()).thenReturn(List.of());
        when(animatriceFormationRepository.count()).thenReturn(15L);

        Map<String, Object> stats = service.getStatsFormations();

        assertThat(stats).containsKey("total");
        assertThat(stats.get("total")).isEqualTo(10L);
        assertThat(stats.get("ouvertes")).isEqualTo(3L);
    }
}