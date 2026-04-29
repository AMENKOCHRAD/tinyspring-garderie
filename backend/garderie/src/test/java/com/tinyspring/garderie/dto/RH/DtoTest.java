package com.tinyspring.garderie.dto.RH;

import com.tinyspring.garderie.entity.RH.enums.StatutAbsenceConge;
import com.tinyspring.garderie.entity.RH.enums.StatutAnimatrice;
import com.tinyspring.garderie.entity.RH.enums.StatutFormation;
import com.tinyspring.garderie.entity.RH.enums.TypeAbsenceConge;
import com.tinyspring.garderie.entity.RH.enums.TypeFormation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Tests DTOs RH")
class DtoTest {

    // ===== AbsenceCongeDTO =====

    @Test
    @DisplayName("AbsenceCongeDTO.builder — doit construire correctement")
    void absenceCongeDTO_builder() {
        LocalDate debut = LocalDate.now();
        LocalDate fin = LocalDate.now().plusDays(5);

        AbsenceCongeDTO dto = AbsenceCongeDTO.builder()
                .id(1L)
                .animatriceId(2L)
                .animatriceNom("Ben Ali")
                .animatricePrenom("Sara")
                .type(TypeAbsenceConge.CONGE_ANNUEL)
                .dateDebut(debut)
                .dateFin(fin)
                .motif("Vacances")
                .statut(StatutAbsenceConge.EN_ATTENTE)
                .nbJours(5)
                .decisionAutomatique(false)
                .motifDecision("En attente")
                .build();

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getAnimatriceId()).isEqualTo(2L);
        assertThat(dto.getAnimatriceNom()).isEqualTo("Ben Ali");
        assertThat(dto.getType()).isEqualTo(TypeAbsenceConge.CONGE_ANNUEL);
        assertThat(dto.getDateDebut()).isEqualTo(debut);
        assertThat(dto.getNbJours()).isEqualTo(5);
        assertThat(dto.getStatut()).isEqualTo(StatutAbsenceConge.EN_ATTENTE);
        assertThat(dto.getDecisionAutomatique()).isFalse();
    }

    @Test
    @DisplayName("AbsenceCongeDTO — setResultatEvaluation doit fonctionner")
    void absenceCongeDTO_setResultatEvaluation() {
        ResultatEvaluationDTO resultat = ResultatEvaluationDTO.builder()
                .decision("AUTO_APPROUVE")
                .explication("OK")
                .build();

        AbsenceCongeDTO dto = new AbsenceCongeDTO();
        dto.setResultatEvaluation(resultat);

        assertThat(dto.getResultatEvaluation()).isNotNull();
        assertThat(dto.getResultatEvaluation().getDecision()).isEqualTo("AUTO_APPROUVE");
    }

    // ===== AnimatriceDTO =====

    @Test
    @DisplayName("AnimatriceDTO.builder — doit construire correctement")
    void animatriceDTO_builder() {
        AnimatriceDTO dto = AnimatriceDTO.builder()
                .id(1L)
                .nom("Ben Ali")
                .prenom("Sara")
                .email("sara@tinyspring.com")
                .telephone("22334455")
                .dateEmbauche(LocalDate.now())
                .statut(StatutAnimatrice.ACTIVE)
                .specialite("premiers secours")
                .photoUrl("photo.jpg")
                .motDePasseTemporaire("temp123")
                .build();

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getNom()).isEqualTo("Ben Ali");
        assertThat(dto.getEmail()).isEqualTo("sara@tinyspring.com");
        assertThat(dto.getStatut()).isEqualTo(StatutAnimatrice.ACTIVE);
        assertThat(dto.getMotDePasseTemporaire()).isEqualTo("temp123");
    }

    // ===== NotificationDTO =====

    @Test
    @DisplayName("NotificationDTO.builder — doit construire correctement")
    void notificationDTO_builder() {
        LocalDateTime now = LocalDateTime.now();

        NotificationDTO dto = NotificationDTO.builder()
                .id(1L)
                .message("Test message")
                .type("ABSENCE")
                .read(false)
                .createdAt(now)
                .build();

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getMessage()).isEqualTo("Test message");
        assertThat(dto.getType()).isEqualTo("ABSENCE");
        assertThat(dto.isRead()).isFalse();
        assertThat(dto.getCreatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("NotificationDTO — setters doivent fonctionner")
    void notificationDTO_setters() {
        NotificationDTO dto = new NotificationDTO();
        dto.setMessage("Hello");
        dto.setType("FORMATION");
        dto.setRead(true);

        assertThat(dto.getMessage()).isEqualTo("Hello");
        assertThat(dto.isRead()).isTrue();
    }

    // ===== ResultatEvaluationDTO =====

    @Test
    @DisplayName("ResultatEvaluationDTO.builder — doit construire correctement")
    void resultatEvaluationDTO_builder() {
        ResultatEvaluationDTO dto = ResultatEvaluationDTO.builder()
                .decision("AUTO_APPROUVE")
                .regleDeclenchee("TOUTES_REGLES_OK")
                .explication("Toutes les règles passées")
                .joursDejaUtilises(10)
                .joursRestants(20)
                .quotaMax(30)
                .build();

        assertThat(dto.getDecision()).isEqualTo("AUTO_APPROUVE");
        assertThat(dto.getRegleDeclenchee()).isEqualTo("TOUTES_REGLES_OK");
        assertThat(dto.getJoursDejaUtilises()).isEqualTo(10);
        assertThat(dto.getJoursRestants()).isEqualTo(20);
        assertThat(dto.getQuotaMax()).isEqualTo(30);
    }

    // ===== DashboardStatsDTO =====

    @Test
    @DisplayName("DashboardStatsDTO.builder — doit construire correctement")
    void dashboardStatsDTO_builder() {
        DashboardStatsDTO dto = DashboardStatsDTO.builder()
                .totalAnimatrices(5L)
                .animatricesActives(4L)
                .animatricesInactives(1L)
                .totalAbsences(10L)
                .absencesEnAttente(3L)
                .absencesApprouvees(5L)
                .absencesRefusees(2L)
                .absences(1L)
                .congesAnnuels(6L)
                .congesMaladie(2L)
                .congesMaternite(1L)
                .totalFormations(8L)
                .formationsInscrites(3L)
                .formationsEnCours(2L)
                .formationsTerminees(3L)
                .dernieresDemandesEnAttente(List.of(new AbsenceCongeDTO()))
                .dernieresAnimatrices(List.of(new AnimatriceDTO()))
                .build();

        assertThat(dto.getTotalAnimatrices()).isEqualTo(5L);
        assertThat(dto.getAbsencesEnAttente()).isEqualTo(3L);
        assertThat(dto.getTotalFormations()).isEqualTo(8L);
        assertThat(dto.getDernieresDemandesEnAttente()).hasSize(1);
        assertThat(dto.getDernieresAnimatrices()).hasSize(1);
    }

    // ===== RapportRHDTO =====

    @Test
    @DisplayName("RapportRHDTO.builder — doit construire correctement")
    void rapportRHDTO_builder() {
        LocalDateTime now = LocalDateTime.now();

        RapportRHDTO dto = RapportRHDTO.builder()
                .id(1L)
                .question("Rapport des absences du mois")
                .typeRapport("MENSUEL")
                .periode("Mois en cours")
                .contenu("Contenu du rapport généré")
                .dateGeneration(now)
                .build();

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getQuestion()).isEqualTo("Rapport des absences du mois");
        assertThat(dto.getTypeRapport()).isEqualTo("MENSUEL");
        assertThat(dto.getContenu()).isEqualTo("Contenu du rapport généré");
        assertThat(dto.getDateGeneration()).isEqualTo(now);
    }

    // ===== FormationDTO =====

    @Test
    @DisplayName("FormationDTO — setters et getters doivent fonctionner")
    void formationDTO_settersGetters() {
        // ✅ type est TypeFormation enum, statutInscription est StatutFormation enum
        FormationDTO dto = new FormationDTO();
        dto.setTitre("Formation Test");
        dto.setType(TypeFormation.SECOURISME);
        dto.setFormateur("Dr. Martin");
        dto.setStatutInscription(StatutFormation.OUVERTE);
        dto.setPlacesMax(20);

        assertThat(dto.getTitre()).isEqualTo("Formation Test");
        assertThat(dto.getType()).isEqualTo(TypeFormation.SECOURISME);
        assertThat(dto.getFormateur()).isEqualTo("Dr. Martin");
        assertThat(dto.getStatutInscription()).isEqualTo(StatutFormation.OUVERTE);
        assertThat(dto.getPlacesMax()).isEqualTo(20);
    }

    @Test
    @DisplayName("FormationDTO.builder — doit construire correctement")
    void formationDTO_builder() {
        FormationDTO dto = FormationDTO.builder()
                .id(1L)
                .titre("Secourisme")
                .type(TypeFormation.SECOURISME)
                .formateur("Dr. Martin")
                .placesMax(15)
                .statutInscription(StatutFormation.OUVERTE)
                .build();

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getTitre()).isEqualTo("Secourisme");
        assertThat(dto.getType()).isEqualTo(TypeFormation.SECOURISME);
        assertThat(dto.getPlacesMax()).isEqualTo(15);
    }
}