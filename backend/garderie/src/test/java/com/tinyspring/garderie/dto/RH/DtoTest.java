package com.tinyspring.garderie.dto.RH;

import com.tinyspring.garderie.dto.LoginRequest;
import com.tinyspring.garderie.entity.RH.enums.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Tests complets tous les DTOs")
class DtoTest {

    // ══════════════════════════════════════════
    // AbsenceCongeDTO
    // ══════════════════════════════════════════

    @Test @DisplayName("AbsenceCongeDTO — builder complet")
    void absenceCongeDTO_builder() {
        ResultatEvaluationDTO res = ResultatEvaluationDTO.builder().decision("AUTO_APPROUVE").build();
        AbsenceCongeDTO dto = AbsenceCongeDTO.builder()
                .id(1L).animatriceId(2L)
                .animatriceNom("Ben Ali").animatricePrenom("Sara")
                .type(TypeAbsenceConge.CONGE_ANNUEL)
                .dateDebut(LocalDate.now()).dateFin(LocalDate.now().plusDays(5))
                .motif("Vacances").statut(StatutAbsenceConge.EN_ATTENTE)
                .nbJours(5).decisionAutomatique(false)
                .motifDecision("En attente").resultatEvaluation(res).build();

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getAnimatriceId()).isEqualTo(2L);
        assertThat(dto.getAnimatriceNom()).isEqualTo("Ben Ali");
        assertThat(dto.getAnimatricePrenom()).isEqualTo("Sara");
        assertThat(dto.getType()).isEqualTo(TypeAbsenceConge.CONGE_ANNUEL);
        assertThat(dto.getMotif()).isEqualTo("Vacances");
        assertThat(dto.getStatut()).isEqualTo(StatutAbsenceConge.EN_ATTENTE);
        assertThat(dto.getNbJours()).isEqualTo(5);
        assertThat(dto.getDecisionAutomatique()).isFalse();
        assertThat(dto.getMotifDecision()).isEqualTo("En attente");
        assertThat(dto.getResultatEvaluation()).isNotNull();
    }

    @Test @DisplayName("AbsenceCongeDTO — no-args + setters")
    void absenceCongeDTO_setters() {
        AbsenceCongeDTO dto = new AbsenceCongeDTO();
        dto.setId(10L); dto.setAnimatriceId(5L);
        dto.setAnimatriceNom("Dupont"); dto.setAnimatricePrenom("Marie");
        dto.setType(TypeAbsenceConge.ABSENCE);
        dto.setDateDebut(LocalDate.now()); dto.setDateFin(LocalDate.now().plusDays(1));
        dto.setMotif("Urgence"); dto.setStatut(StatutAbsenceConge.APPROUVE);
        dto.setNbJours(1); dto.setDecisionAutomatique(true);
        dto.setMotifDecision("Auto"); dto.setResultatEvaluation(null);
        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getStatut()).isEqualTo(StatutAbsenceConge.APPROUVE);
        assertThat(dto.getDecisionAutomatique()).isTrue();
    }

    @Test @DisplayName("AbsenceCongeDTO — constructeur AllArgs")
    void absenceCongeDTO_allArgs() {
        AbsenceCongeDTO dto = new AbsenceCongeDTO(
                1L, 2L, "Ben Ali", "Sara",
                TypeAbsenceConge.CONGE_MALADIE,
                LocalDate.now(), LocalDate.now().plusDays(3),
                "Maladie", StatutAbsenceConge.REFUSE, 3,
                true, "Quota dépassé", null
        );
        assertThat(dto.getType()).isEqualTo(TypeAbsenceConge.CONGE_MALADIE);
        assertThat(dto.getStatut()).isEqualTo(StatutAbsenceConge.REFUSE);
        assertThat(dto.getDecisionAutomatique()).isTrue();
    }

    // ══════════════════════════════════════════
    // AnimatriceDTO
    // ══════════════════════════════════════════

    @Test @DisplayName("AnimatriceDTO — builder complet")
    void animatriceDTO_builder() {
        AnimatriceDTO dto = AnimatriceDTO.builder()
                .id(1L).nom("Ben Ali").prenom("Sara")
                .email("sara@tinyspring.com").telephone("22334455")
                .dateEmbauche(LocalDate.now()).statut(StatutAnimatrice.ACTIVE)
                .specialite("premiers secours").photoUrl("photo.jpg")
                .motDePasseTemporaire("temp123").build();

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getNom()).isEqualTo("Ben Ali");
        assertThat(dto.getPrenom()).isEqualTo("Sara");
        assertThat(dto.getEmail()).isEqualTo("sara@tinyspring.com");
        assertThat(dto.getTelephone()).isEqualTo("22334455");
        assertThat(dto.getStatut()).isEqualTo(StatutAnimatrice.ACTIVE);
        assertThat(dto.getSpecialite()).isEqualTo("premiers secours");
        assertThat(dto.getPhotoUrl()).isEqualTo("photo.jpg");
        assertThat(dto.getMotDePasseTemporaire()).isEqualTo("temp123");
    }

    @Test @DisplayName("AnimatriceDTO — no-args + setters")
    void animatriceDTO_setters() {
        AnimatriceDTO dto = new AnimatriceDTO();
        dto.setId(5L); dto.setNom("Trabelsi"); dto.setPrenom("Amira");
        dto.setEmail("amira@test.com"); dto.setTelephone("11223344");
        dto.setDateEmbauche(LocalDate.now()); dto.setStatut(StatutAnimatrice.INACTIVE);
        dto.setSpecialite("arts plastiques"); dto.setPhotoUrl(null);
        dto.setMotDePasseTemporaire(null);
        assertThat(dto.getNom()).isEqualTo("Trabelsi");
        assertThat(dto.getStatut()).isEqualTo(StatutAnimatrice.INACTIVE);
        assertThat(dto.getPhotoUrl()).isNull();
    }

    @Test @DisplayName("AnimatriceDTO — constructeur AllArgs")
    void animatriceDTO_allArgs() {
        AnimatriceDTO dto = new AnimatriceDTO(
                1L, "Ben Ali", "Sara", "sara@test.com",
                "22334455", LocalDate.now(), StatutAnimatrice.ACTIVE,
                "secourisme", "photo.jpg", "pass123"
        );
        assertThat(dto.getEmail()).isEqualTo("sara@test.com");
        assertThat(dto.getMotDePasseTemporaire()).isEqualTo("pass123");
    }

    // ══════════════════════════════════════════
    // NotificationDTO
    // ══════════════════════════════════════════

    @Test @DisplayName("NotificationDTO — builder complet")
    void notificationDTO_builder() {
        LocalDateTime now = LocalDateTime.now();
        NotificationDTO dto = NotificationDTO.builder()
                .id(1L).message("Test").type("ABSENCE")
                .read(false).createdAt(now).build();
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getMessage()).isEqualTo("Test");
        assertThat(dto.getType()).isEqualTo("ABSENCE");
        assertThat(dto.isRead()).isFalse();
        assertThat(dto.getCreatedAt()).isEqualTo(now);
    }

    @Test @DisplayName("NotificationDTO — no-args + setters")
    void notificationDTO_setters() {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(2L); dto.setMessage("Formation");
        dto.setType("FORMATION"); dto.setRead(true);
        dto.setCreatedAt(LocalDateTime.now());
        assertThat(dto.getMessage()).isEqualTo("Formation");
        assertThat(dto.isRead()).isTrue();
    }

    @Test @DisplayName("NotificationDTO — constructeur AllArgs")
    void notificationDTO_allArgs() {
        LocalDateTime now = LocalDateTime.now();
        NotificationDTO dto = new NotificationDTO(1L, "msg", "TYPE", true, now);
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.isRead()).isTrue();
    }

    // ══════════════════════════════════════════
    // ResultatEvaluationDTO
    // ══════════════════════════════════════════

    @Test @DisplayName("ResultatEvaluationDTO — builder complet")
    void resultatEvaluationDTO_builder() {
        ResultatEvaluationDTO dto = ResultatEvaluationDTO.builder()
                .decision("AUTO_APPROUVE").regleDeclenchee("TOUTES_REGLES_OK")
                .explication("OK").joursDejaUtilises(10)
                .joursRestants(20).quotaMax(30).build();
        assertThat(dto.getDecision()).isEqualTo("AUTO_APPROUVE");
        assertThat(dto.getRegleDeclenchee()).isEqualTo("TOUTES_REGLES_OK");
        assertThat(dto.getExplication()).isEqualTo("OK");
        assertThat(dto.getJoursDejaUtilises()).isEqualTo(10);
        assertThat(dto.getJoursRestants()).isEqualTo(20);
        assertThat(dto.getQuotaMax()).isEqualTo(30);
    }

    @Test @DisplayName("ResultatEvaluationDTO — no-args + setters")
    void resultatEvaluationDTO_setters() {
        ResultatEvaluationDTO dto = new ResultatEvaluationDTO();
        dto.setDecision("AUTO_REFUSE"); dto.setRegleDeclenchee("DELAI_PREVENANCE");
        dto.setExplication("Délai insuffisant");
        dto.setJoursDejaUtilises(5); dto.setJoursRestants(25); dto.setQuotaMax(30);
        assertThat(dto.getDecision()).isEqualTo("AUTO_REFUSE");
        assertThat(dto.getRegleDeclenchee()).isEqualTo("DELAI_PREVENANCE");
    }

    @Test @DisplayName("ResultatEvaluationDTO — constructeur AllArgs")
    void resultatEvaluationDTO_allArgs() {
        ResultatEvaluationDTO dto = new ResultatEvaluationDTO(
                "TRANSMIS_ADMIN", "OK", "Manuel", 5, 25, 30);
        assertThat(dto.getDecision()).isEqualTo("TRANSMIS_ADMIN");
        assertThat(dto.getJoursRestants()).isEqualTo(25);
    }

    // ══════════════════════════════════════════
    // DashboardStatsDTO
    // ══════════════════════════════════════════

    @Test @DisplayName("DashboardStatsDTO — builder complet")
    void dashboardStatsDTO_builder() {
        DashboardStatsDTO dto = DashboardStatsDTO.builder()
                .totalAnimatrices(5L).animatricesActives(4L).animatricesInactives(1L)
                .totalAbsences(10L).absencesEnAttente(3L)
                .absencesApprouvees(5L).absencesRefusees(2L)
                .absences(1L).congesAnnuels(6L).congesMaladie(2L).congesMaternite(1L)
                .totalFormations(8L).formationsInscrites(3L)
                .formationsEnCours(2L).formationsTerminees(3L)
                .dernieresDemandesEnAttente(List.of())
                .dernieresAnimatrices(List.of()).build();
        assertThat(dto.getTotalAnimatrices()).isEqualTo(5L);
        assertThat(dto.getAnimatricesActives()).isEqualTo(4L);
        assertThat(dto.getAbsencesEnAttente()).isEqualTo(3L);
        assertThat(dto.getCongesAnnuels()).isEqualTo(6L);
        assertThat(dto.getTotalFormations()).isEqualTo(8L);
    }

    @Test @DisplayName("DashboardStatsDTO — no-args + setters")
    void dashboardStatsDTO_setters() {
        DashboardStatsDTO dto = new DashboardStatsDTO();
        dto.setTotalAnimatrices(3L); dto.setAnimatricesActives(2L);
        dto.setAnimatricesInactives(1L); dto.setTotalAbsences(5L);
        dto.setAbsencesEnAttente(1L); dto.setAbsencesApprouvees(3L);
        dto.setAbsencesRefusees(1L); dto.setAbsences(0L);
        dto.setCongesAnnuels(3L); dto.setCongesMaladie(1L); dto.setCongesMaternite(1L);
        dto.setTotalFormations(4L); dto.setFormationsInscrites(2L);
        dto.setFormationsEnCours(1L); dto.setFormationsTerminees(1L);
        dto.setDernieresDemandesEnAttente(List.of());
        dto.setDernieresAnimatrices(List.of());
        assertThat(dto.getTotalAnimatrices()).isEqualTo(3L);
        assertThat(dto.getCongesAnnuels()).isEqualTo(3L);
    }

    // ══════════════════════════════════════════
    // RapportRHDTO
    // ══════════════════════════════════════════

    @Test @DisplayName("RapportRHDTO — builder complet")
    void rapportRHDTO_builder() {
        LocalDateTime now = LocalDateTime.now();
        RapportRHDTO dto = RapportRHDTO.builder()
                .id(1L).question("Rapport mensuel").typeRapport("MENSUEL")
                .periode("Mois en cours").contenu("Contenu").dateGeneration(now).build();
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getQuestion()).isEqualTo("Rapport mensuel");
        assertThat(dto.getTypeRapport()).isEqualTo("MENSUEL");
        assertThat(dto.getPeriode()).isEqualTo("Mois en cours");
        assertThat(dto.getContenu()).isEqualTo("Contenu");
        assertThat(dto.getDateGeneration()).isEqualTo(now);
    }

    @Test @DisplayName("RapportRHDTO — no-args + setters")
    void rapportRHDTO_setters() {
        RapportRHDTO dto = new RapportRHDTO();
        dto.setId(2L); dto.setQuestion("Bilan"); dto.setTypeRapport("ANNUEL");
        dto.setPeriode("2026"); dto.setContenu("Données"); dto.setDateGeneration(LocalDateTime.now());
        assertThat(dto.getTypeRapport()).isEqualTo("ANNUEL");
        assertThat(dto.getPeriode()).isEqualTo("2026");
    }

    @Test @DisplayName("RapportRHDTO — constructeur AllArgs")
    void rapportRHDTO_allArgs() {
        LocalDateTime now = LocalDateTime.now();
        RapportRHDTO dto = new RapportRHDTO(1L, "Q", "GENERAL", "Période", "Contenu", now);
        assertThat(dto.getTypeRapport()).isEqualTo("GENERAL");
        assertThat(dto.getDateGeneration()).isEqualTo(now);
    }

    // ══════════════════════════════════════════
    // FormationDTO
    // ══════════════════════════════════════════

    @Test @DisplayName("FormationDTO — builder complet")
    void formationDTO_builder() {
        FormationDTO dto = FormationDTO.builder()
                .id(1L).titre("Secourisme").type(TypeFormation.SECOURISME)
                .description("Formation secourisme").formateur("Dr. Martin")
                .dateDebut(LocalDate.now()).dateFin(LocalDate.now().plusDays(1))
                .placesMax(15).statutInscription(StatutFormation.OUVERTE)
                .animatrices(List.of()).build();
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getTitre()).isEqualTo("Secourisme");
        assertThat(dto.getDescription()).isEqualTo("Formation secourisme");
        assertThat(dto.getStatutInscription()).isEqualTo(StatutFormation.OUVERTE);
        assertThat(dto.getAnimatrices()).isEmpty();
    }

    @Test @DisplayName("FormationDTO — no-args + setters complets")
    void formationDTO_setters() {
        FormationDTO dto = new FormationDTO();
        dto.setId(2L); dto.setTitre("Pédagogie");
        dto.setType(TypeFormation.PEDAGOGIE); dto.setDescription("Cours pédagogie");
        dto.setFormateur("Mme Dupont");
        dto.setDateDebut(LocalDate.now()); dto.setDateFin(LocalDate.now().plusDays(2));
        dto.setPlacesMax(10); dto.setStatutInscription(StatutFormation.EN_COURS);
        dto.setAnimatrices(List.of());
        assertThat(dto.getTitre()).isEqualTo("Pédagogie");
        assertThat(dto.getType()).isEqualTo(TypeFormation.PEDAGOGIE);
        assertThat(dto.getStatutInscription()).isEqualTo(StatutFormation.EN_COURS);
    }

    // ══════════════════════════════════════════
    // CalendrierEventDTO
    // ══════════════════════════════════════════

    @Test @DisplayName("CalendrierEventDTO — builder complet")
    void calendrierEventDTO_builder() {
        CalendrierEventDTO dto = CalendrierEventDTO.builder()
                .id("abs-1").title("Sara — CONGE ANNUEL")
                .start("2026-05-01").end("2026-05-06")
                .color("#10b981").type("ABSENCE").build();
        assertThat(dto.getId()).isEqualTo("abs-1");
        assertThat(dto.getTitle()).isEqualTo("Sara — CONGE ANNUEL");
        assertThat(dto.getStart()).isEqualTo("2026-05-01");
        assertThat(dto.getEnd()).isEqualTo("2026-05-06");
        assertThat(dto.getColor()).isEqualTo("#10b981");
        assertThat(dto.getType()).isEqualTo("ABSENCE");
    }

    @Test @DisplayName("CalendrierEventDTO — no-args + setters")
    void calendrierEventDTO_setters() {
        CalendrierEventDTO dto = new CalendrierEventDTO();
        dto.setId("form-1"); dto.setTitle("Secourisme");
        dto.setStart("2026-06-01"); dto.setEnd("2026-06-02");
        dto.setColor("#6366f1"); dto.setType("FORMATION");
        assertThat(dto.getId()).isEqualTo("form-1");
        assertThat(dto.getType()).isEqualTo("FORMATION");
    }

    @Test @DisplayName("CalendrierEventDTO — constructeur AllArgs")
    void calendrierEventDTO_allArgs() {
        CalendrierEventDTO dto = new CalendrierEventDTO(
                "id-1", "Titre", "2026-01-01", "2026-01-02", "#fff", "ABSENCE");
        assertThat(dto.getId()).isEqualTo("id-1");
        assertThat(dto.getColor()).isEqualTo("#fff");
    }

    // ══════════════════════════════════════════
    // QuotaCongeDTO
    // ══════════════════════════════════════════

    @Test @DisplayName("QuotaCongeDTO — builder complet")
    void quotaCongeDTO_builder() {
        QuotaCongeDTO dto = QuotaCongeDTO.builder()
                .id(1L).type(TypeAbsenceConge.CONGE_ANNUEL)
                .nbJoursMax(30).delaiPrevenanceJours(7)
                .effectifMinimum(2).autoApprobation(true)
                .joursUtilisesAnneeEnCours(10).joursRestants(20).build();
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getType()).isEqualTo(TypeAbsenceConge.CONGE_ANNUEL);
        assertThat(dto.getNbJoursMax()).isEqualTo(30);
        assertThat(dto.getDelaiPrevenanceJours()).isEqualTo(7);
        assertThat(dto.getEffectifMinimum()).isEqualTo(2);
        assertThat(dto.isAutoApprobation()).isTrue();
        assertThat(dto.getJoursUtilisesAnneeEnCours()).isEqualTo(10);
        assertThat(dto.getJoursRestants()).isEqualTo(20);
    }

    @Test @DisplayName("QuotaCongeDTO — no-args + setters")
    void quotaCongeDTO_setters() {
        QuotaCongeDTO dto = new QuotaCongeDTO();
        dto.setId(2L); dto.setType(TypeAbsenceConge.CONGE_MALADIE);
        dto.setNbJoursMax(15); dto.setDelaiPrevenanceJours(0);
        dto.setEffectifMinimum(1); dto.setAutoApprobation(false);
        dto.setJoursUtilisesAnneeEnCours(3); dto.setJoursRestants(12);
        assertThat(dto.getType()).isEqualTo(TypeAbsenceConge.CONGE_MALADIE);
        assertThat(dto.isAutoApprobation()).isFalse();
        assertThat(dto.getJoursRestants()).isEqualTo(12);
    }

    @Test @DisplayName("QuotaCongeDTO — constructeur AllArgs")
    void quotaCongeDTO_allArgs() {
        QuotaCongeDTO dto = new QuotaCongeDTO(
                1L, TypeAbsenceConge.CONGE_MATERNITE, 90, 30, 1, true, 0, 90);
        assertThat(dto.getType()).isEqualTo(TypeAbsenceConge.CONGE_MATERNITE);
        assertThat(dto.getNbJoursMax()).isEqualTo(90);
        assertThat(dto.getJoursRestants()).isEqualTo(90);
    }

    // ══════════════════════════════════════════
    // RapportRequestDTO
    // ══════════════════════════════════════════

    @Test @DisplayName("RapportRequestDTO — no-args + setter")
    void rapportRequestDTO_setters() {
        RapportRequestDTO dto = new RapportRequestDTO();
        dto.setQuestion("Rapport mensuel absences");
        assertThat(dto.getQuestion()).isEqualTo("Rapport mensuel absences");
    }

    @Test @DisplayName("RapportRequestDTO — constructeur AllArgs")
    void rapportRequestDTO_allArgs() {
        RapportRequestDTO dto = new RapportRequestDTO("Bilan annuel");
        assertThat(dto.getQuestion()).isEqualTo("Bilan annuel");
    }

    // ══════════════════════════════════════════
    // LoginRequest
    // ══════════════════════════════════════════

    @Test @DisplayName("LoginRequest — setters et getters")
    void loginRequest_settersGetters() {
        LoginRequest req = new LoginRequest();
        req.setEmail("admin@test.com"); req.setPassword("secret123");
        assertThat(req.getEmail()).isEqualTo("admin@test.com");
        assertThat(req.getPassword()).isEqualTo("secret123");
    }

    @Test @DisplayName("LoginRequest — valeurs null acceptées")
    void loginRequest_valeurNull() {
        LoginRequest req = new LoginRequest();
        assertThat(req.getEmail()).isNull();
        assertThat(req.getPassword()).isNull();
    }

    // ══════════════════════════════════════════
    // Enums
    // ══════════════════════════════════════════

    @Test @DisplayName("TypeAbsenceConge — toutes les valeurs")
    void typeAbsenceConge_enum() {
        assertThat(TypeAbsenceConge.values())
                .contains(TypeAbsenceConge.ABSENCE, TypeAbsenceConge.CONGE_ANNUEL,
                        TypeAbsenceConge.CONGE_MALADIE, TypeAbsenceConge.CONGE_MATERNITE);
        assertThat(TypeAbsenceConge.valueOf("CONGE_ANNUEL")).isEqualTo(TypeAbsenceConge.CONGE_ANNUEL);
    }

    @Test @DisplayName("StatutAbsenceConge — toutes les valeurs")
    void statutAbsenceConge_enum() {
        assertThat(StatutAbsenceConge.values())
                .contains(StatutAbsenceConge.EN_ATTENTE,
                        StatutAbsenceConge.APPROUVE, StatutAbsenceConge.REFUSE);
    }

    @Test @DisplayName("StatutAnimatrice — toutes les valeurs")
    void statutAnimatrice_enum() {
        assertThat(StatutAnimatrice.values())
                .contains(StatutAnimatrice.ACTIVE, StatutAnimatrice.INACTIVE);
    }

    @Test @DisplayName("StatutFormation — toutes les valeurs")
    void statutFormation_enum() {
        assertThat(StatutFormation.values())
                .contains(StatutFormation.OUVERTE, StatutFormation.EN_COURS,
                        StatutFormation.TERMINEE, StatutFormation.ANNULEE);
    }

    @Test @DisplayName("TypeFormation — toutes les valeurs")
    void typeFormation_enum() {
        assertThat(TypeFormation.values()).hasSizeGreaterThan(0);
        assertThat(TypeFormation.valueOf("SECOURISME")).isEqualTo(TypeFormation.SECOURISME);
    }

    @Test @DisplayName("StatutInscription — toutes les valeurs")
    void statutInscription_enum() {
        assertThat(StatutInscription.values())
                .contains(StatutInscription.INSCRITE, StatutInscription.LISTE_ATTENTE,
                        StatutInscription.TERMINEE, StatutInscription.ABANDONNEE);
    }

    @Test @DisplayName("StatutValidite — toutes les valeurs")
    void statutValidite_enum() {
        assertThat(StatutValidite.values())
                .contains(StatutValidite.VALIDE,
                        StatutValidite.BIENTOT_EXPIREE, StatutValidite.EXPIREE);
    }
}