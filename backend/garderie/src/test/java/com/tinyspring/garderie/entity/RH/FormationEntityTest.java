package com.tinyspring.garderie.entity.RH;

import com.tinyspring.garderie.entity.RH.enums.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Tests Entités RH")
class FormationEntityTest {

    // ===== Formation.getNbInscrits() =====

    @Test
    @DisplayName("Formation.getNbInscrits — doit retourner 0 si inscriptions null")
    void getNbInscrits_doitRetourner0SiInscriptionsNull() {
        Formation f = new Formation();
        f.setInscriptions(null);
        assertThat(f.getNbInscrits()).isEqualTo(0);
    }

    @Test
    @DisplayName("Formation.getNbInscrits — doit retourner 0 si liste vide")
    void getNbInscrits_doitRetourner0SiListeVide() {
        Formation f = new Formation();
        f.setInscriptions(List.of());
        assertThat(f.getNbInscrits()).isEqualTo(0);
    }

    @Test
    @DisplayName("Formation.getNbInscrits — doit compter uniquement les INSCRITE")
    void getNbInscrits_doitCompterUniquementInscrites() {
        AnimatriceFormation ins1 = new AnimatriceFormation();
        ins1.setStatut(StatutInscription.INSCRITE);

        AnimatriceFormation ins2 = new AnimatriceFormation();
        ins2.setStatut(StatutInscription.INSCRITE);

        AnimatriceFormation attente = new AnimatriceFormation();
        attente.setStatut(StatutInscription.LISTE_ATTENTE);

        AnimatriceFormation abandonnee = new AnimatriceFormation();
        abandonnee.setStatut(StatutInscription.ABANDONNEE);

        Formation f = new Formation();
        f.setInscriptions(List.of(ins1, ins2, attente, abandonnee));

        assertThat(f.getNbInscrits()).isEqualTo(2);
    }

    // ===== Formation.getPlacesDisponibles() =====

    @Test
    @DisplayName("Formation.getPlacesDisponibles — doit retourner 999 si placesMax null")
    void getPlacesDisponibles_doitRetourner999SiPlacesMaxNull() {
        Formation f = new Formation();
        f.setPlacesMax(null);
        f.setInscriptions(List.of());
        assertThat(f.getPlacesDisponibles()).isEqualTo(999);
    }

    @Test
    @DisplayName("Formation.getPlacesDisponibles — doit retourner placesMax - inscrits")
    void getPlacesDisponibles_doitRetournerPlacesRestantes() {
        AnimatriceFormation ins = new AnimatriceFormation();
        ins.setStatut(StatutInscription.INSCRITE);

        Formation f = new Formation();
        f.setPlacesMax(10);
        f.setInscriptions(List.of(ins, ins, ins)); // 3 inscrits

        assertThat(f.getPlacesDisponibles()).isEqualTo(7);
    }

    @Test
    @DisplayName("Formation.getPlacesDisponibles — doit retourner 0 si complet")
    void getPlacesDisponibles_doitRetourner0SiComplet() {
        AnimatriceFormation ins = new AnimatriceFormation();
        ins.setStatut(StatutInscription.INSCRITE);

        Formation f = new Formation();
        f.setPlacesMax(2);
        f.setInscriptions(List.of(ins, ins)); // 2/2

        assertThat(f.getPlacesDisponibles()).isEqualTo(0);
    }

    // ===== Formation.isComplet() =====

    @Test
    @DisplayName("Formation.isComplet — doit retourner false si placesMax null")
    void isComplet_doitRetournerFalseSiPlacesMaxNull() {
        Formation f = new Formation();
        f.setPlacesMax(null);
        f.setInscriptions(List.of());
        assertThat(f.isComplet()).isFalse();
    }

    @Test
    @DisplayName("Formation.isComplet — doit retourner true si places épuisées")
    void isComplet_doitRetournerTrueSiPlacesEpuisees() {
        AnimatriceFormation ins = new AnimatriceFormation();
        ins.setStatut(StatutInscription.INSCRITE);

        Formation f = new Formation();
        f.setPlacesMax(1);
        f.setInscriptions(List.of(ins));

        assertThat(f.isComplet()).isTrue();
    }

    @Test
    @DisplayName("Formation.isComplet — doit retourner false si places disponibles")
    void isComplet_doitRetournerFalseSiPlacesDisponibles() {
        Formation f = new Formation();
        f.setPlacesMax(5);
        f.setInscriptions(List.of());
        assertThat(f.isComplet()).isFalse();
    }

    // ===== Formation Builder =====

    @Test
    @DisplayName("Formation.builder — doit construire correctement")
    void formation_builder_doitConstruireCorrectement() {
        Formation f = Formation.builder()
                .titre("Secourisme")
                .type(TypeFormation.SECOURISME)
                .formateur("Dr. Martin")
                .statut(StatutFormation.OUVERTE)
                .obligatoire(true)
                .placesMax(20)
                .dureeValiditeMois(12)
                .build();

        assertThat(f.getTitre()).isEqualTo("Secourisme");
        assertThat(f.getType()).isEqualTo(TypeFormation.SECOURISME);
        assertThat(f.getStatut()).isEqualTo(StatutFormation.OUVERTE);
        assertThat(f.getObligatoire()).isTrue();
        assertThat(f.getPlacesMax()).isEqualTo(20);
    }

    // ===== AnimatriceFormation.calculerStatutValidite() =====

    @Test
    @DisplayName("AnimatriceFormation.calculerStatutValidite — VALIDE si dateExpiration null")
    void calculerStatutValidite_doitEtreValideSiExpirationNull() {
        AnimatriceFormation af = new AnimatriceFormation();
        af.setDateExpiration(null);

        af.calculerStatutValidite();

        assertThat(af.getStatutValidite()).isEqualTo(StatutValidite.VALIDE);
    }

    @Test
    @DisplayName("AnimatriceFormation.calculerStatutValidite — EXPIREE si date passée")
    void calculerStatutValidite_doitEtreExpiree() {
        AnimatriceFormation af = new AnimatriceFormation();
        af.setDateExpiration(LocalDate.now().minusDays(1)); // hier

        af.calculerStatutValidite();

        assertThat(af.getStatutValidite()).isEqualTo(StatutValidite.EXPIREE);
    }

    @Test
    @DisplayName("AnimatriceFormation.calculerStatutValidite — BIENTOT_EXPIREE si < 3 mois")
    void calculerStatutValidite_doitEtreBientotExpiree() {
        AnimatriceFormation af = new AnimatriceFormation();
        af.setDateExpiration(LocalDate.now().plusMonths(1)); // dans 1 mois

        af.calculerStatutValidite();

        assertThat(af.getStatutValidite()).isEqualTo(StatutValidite.BIENTOT_EXPIREE);
    }

    @Test
    @DisplayName("AnimatriceFormation.calculerStatutValidite — VALIDE si > 3 mois")
    void calculerStatutValidite_doitEtreValide() {
        AnimatriceFormation af = new AnimatriceFormation();
        af.setDateExpiration(LocalDate.now().plusMonths(6)); // dans 6 mois

        af.calculerStatutValidite();

        assertThat(af.getStatutValidite()).isEqualTo(StatutValidite.VALIDE);
    }

    // ===== AnimatriceFormation Builder =====

    @Test
    @DisplayName("AnimatriceFormation.builder — doit construire correctement")
    void animatriceFormation_builder_doitConstruireCorrectement() {
        AnimatriceFormation af = AnimatriceFormation.builder()
                .dateInscription(LocalDate.now())
                .statut(StatutInscription.INSCRITE)
                .certificationGeneree(false)
                .build();

        assertThat(af.getStatut()).isEqualTo(StatutInscription.INSCRITE);
        assertThat(af.getCertificationGeneree()).isFalse();
        assertThat(af.getDateInscription()).isEqualTo(LocalDate.now());
    }

    // ===== AbsenceConge Builder =====

    @Test
    @DisplayName("AbsenceConge.builder — doit construire correctement")
    void absenceConge_builder_doitConstruireCorrectement() {
        Animatrice animatrice = new Animatrice();
        animatrice.setId(1L);

        AbsenceConge ac = AbsenceConge.builder()
                .animatrice(animatrice)
                .type(com.tinyspring.garderie.entity.RH.enums.TypeAbsenceConge.CONGE_ANNUEL)
                .dateDebut(LocalDate.now())
                .dateFin(LocalDate.now().plusDays(5))
                .statut(com.tinyspring.garderie.entity.RH.enums.StatutAbsenceConge.EN_ATTENTE)
                .nbJours(5)
                .decisionAutomatique(false)
                .build();

        assertThat(ac.getType()).isEqualTo(com.tinyspring.garderie.entity.RH.enums.TypeAbsenceConge.CONGE_ANNUEL);
        assertThat(ac.getNbJours()).isEqualTo(5);
        assertThat(ac.getStatut()).isEqualTo(com.tinyspring.garderie.entity.RH.enums.StatutAbsenceConge.EN_ATTENTE);
        assertThat(ac.getDecisionAutomatique()).isFalse();
    }

    // ===== HistoriqueDecision.prePersist() =====

    @Test
    @DisplayName("HistoriqueDecision.prePersist — doit initialiser dateDecision")
    void historiqueDecision_prePersist_doitInitialiserDate() {
        HistoriqueDecision hd = new HistoriqueDecision();
        hd.prePersist();
        assertThat(hd.getDateDecision()).isNotNull();
        assertThat(hd.getDateDecision()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    // ===== Notification.prePersist() =====

    @Test
    @DisplayName("Notification.prePersist — doit initialiser createdAt")
    void notification_prePersist_doitInitialiserCreatedAt() {
        Notification n = new Notification();
        n.prePersist();
        assertThat(n.getCreatedAt()).isNotNull();
        assertThat(n.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    // ===== RapportRH.prePersist() =====

    @Test
    @DisplayName("RapportRH.prePersist — doit initialiser dateGeneration")
    void rapportRH_prePersist_doitInitialiserDateGeneration() {
        RapportRH r = new RapportRH();
        r.prePersist();
        assertThat(r.getDateGeneration()).isNotNull();
        assertThat(r.getDateGeneration()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    // ===== QuotaConge Builder =====

    @Test
    @DisplayName("QuotaConge.builder — doit construire correctement")
    void quotaConge_builder_doitConstruireCorrectement() {
        QuotaConge q = QuotaConge.builder()
                .type(com.tinyspring.garderie.entity.RH.enums.TypeAbsenceConge.CONGE_ANNUEL)
                .nbJoursMax(30)
                .delaiPrevenanceJours(7)
                .effectifMinimum(2)
                .autoApprobation(true)
                .build();

        assertThat(q.getNbJoursMax()).isEqualTo(30);
        assertThat(q.getDelaiPrevenanceJours()).isEqualTo(7);
        assertThat(q.getEffectifMinimum()).isEqualTo(2);
        assertThat(q.isAutoApprobation()).isTrue();
    }

    // ===== Animatrice Builder =====

    @Test
    @DisplayName("Animatrice.builder — doit construire correctement")
    void animatrice_builder_doitConstruireCorrectement() {
        Animatrice a = Animatrice.builder()
                .nom("Ben Ali")
                .prenom("Sara")
                .email("sara@test.com")
                .statut(StatutAnimatrice.ACTIVE)
                .specialite("premiers secours")
                .mustChangePassword(true)
                .build();

        assertThat(a.getNom()).isEqualTo("Ben Ali");
        assertThat(a.getEmail()).isEqualTo("sara@test.com");
        assertThat(a.getStatut()).isEqualTo(StatutAnimatrice.ACTIVE);
        assertThat(a.isMustChangePassword()).isTrue();
    }
}
