package com.tinyspring.garderie.service.RH;

import com.tinyspring.garderie.dto.RH.ResultatEvaluationDTO;
import com.tinyspring.garderie.entity.RH.AbsenceConge;
import com.tinyspring.garderie.entity.RH.Animatrice;
import com.tinyspring.garderie.entity.RH.QuotaConge;
import com.tinyspring.garderie.entity.RH.enums.StatutAbsenceConge;
import com.tinyspring.garderie.entity.RH.enums.StatutAnimatrice;
import com.tinyspring.garderie.entity.RH.enums.TypeAbsenceConge;
import com.tinyspring.garderie.repository.RH.AbsenceCongeRepository;
import com.tinyspring.garderie.repository.RH.AnimatriceRepository;
import com.tinyspring.garderie.repository.RH.HistoriqueDecisionRepository;
import com.tinyspring.garderie.repository.RH.QuotaCongeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests MoteurReglesServiceImpl")
class MoteurReglesServiceImplTest {

    @Mock private QuotaCongeRepository quotaCongeRepository;
    @Mock private AbsenceCongeRepository absenceCongeRepository;
    @Mock private HistoriqueDecisionRepository historiqueDecisionRepository;
    @Mock private AnimatriceRepository animatriceRepository;

    @InjectMocks private MoteurReglesServiceImpl moteurRegles;

    private Animatrice animatrice;
    private AbsenceConge demande;
    private QuotaConge quota;

    @BeforeEach
    void setUp() {
        animatrice = new Animatrice();
        animatrice.setId(1L);
        animatrice.setStatut(StatutAnimatrice.ACTIVE);

        demande = AbsenceConge.builder()
                .animatrice(animatrice)
                .type(TypeAbsenceConge.CONGE_ANNUEL)
                .dateDebut(LocalDate.now().plusDays(10))
                .dateFin(LocalDate.now().plusDays(14))
                .nbJours(5)
                .statut(StatutAbsenceConge.EN_ATTENTE)
                .build();
        demande.setId(1L);

        quota = QuotaConge.builder()
                .type(TypeAbsenceConge.CONGE_ANNUEL)
                .nbJoursMax(30)
                .delaiPrevenanceJours(7)
                .effectifMinimum(2)
                .autoApprobation(true)
                .build();
    }

    // ===== Règle 1 : Délai =====
    @Test
    @DisplayName("evaluer — doit refuser si délai insuffisant")
    void evaluer_doitRefuserSiDelaiInsuffisant() {
        demande.setDateDebut(LocalDate.now().plusDays(2));
        demande.setDateFin(LocalDate.now().plusDays(6));

        when(quotaCongeRepository.findByType(any())).thenReturn(Optional.of(quota));
        when(historiqueDecisionRepository.save(any())).thenReturn(null);

        ResultatEvaluationDTO r = moteurRegles.evaluer(demande);

        assertThat(r.getDecision()).isEqualTo("AUTO_REFUSE");
        assertThat(r.getRegleDeclenchee()).isEqualTo("DELAI_PREVENANCE");
    }

    // ===== Règle 2 : Quota =====
    @Test
    @DisplayName("evaluer — doit refuser si quota dépassé")
    void evaluer_doitRefuserSiQuotaDepasse() {
        AbsenceConge existante = AbsenceConge.builder()
                .animatrice(animatrice).type(TypeAbsenceConge.CONGE_ANNUEL)
                .dateDebut(LocalDate.of(LocalDate.now().getYear(), 1, 1))
                .dateFin(LocalDate.of(LocalDate.now().getYear(), 1, 28))
                .nbJours(28).statut(StatutAbsenceConge.APPROUVE).build();
        existante.setId(99L);

        when(quotaCongeRepository.findByType(any())).thenReturn(Optional.of(quota));
        when(absenceCongeRepository.findByAnimatriceId(1L)).thenReturn(List.of(existante));
        when(historiqueDecisionRepository.save(any())).thenReturn(null);

        ResultatEvaluationDTO r = moteurRegles.evaluer(demande);

        assertThat(r.getDecision()).isEqualTo("AUTO_REFUSE");
        assertThat(r.getRegleDeclenchee()).isEqualTo("QUOTA_DEPASSE");
    }

    // ===== Règle 3 : Chevauchement =====
    @Test
    @DisplayName("evaluer — doit refuser si chevauchement")
    void evaluer_doitRefuserSiChevauchement() {
        AbsenceConge existante = AbsenceConge.builder()
                .animatrice(animatrice).type(TypeAbsenceConge.CONGE_ANNUEL)
                .dateDebut(LocalDate.now().plusDays(8))
                .dateFin(LocalDate.now().plusDays(12))
                .nbJours(4).statut(StatutAbsenceConge.APPROUVE).build();
        existante.setId(99L);

        when(quotaCongeRepository.findByType(any())).thenReturn(Optional.of(quota));
        when(absenceCongeRepository.findByAnimatriceId(1L)).thenReturn(List.of(existante));
        when(historiqueDecisionRepository.save(any())).thenReturn(null);

        ResultatEvaluationDTO r = moteurRegles.evaluer(demande);

        assertThat(r.getDecision()).isEqualTo("AUTO_REFUSE");
        assertThat(r.getRegleDeclenchee()).isEqualTo("CHEVAUCHEMENT_DATES");
    }

    // ===== Règle 4 : Effectif =====
    @Test
    @DisplayName("evaluer — doit refuser si effectif insuffisant")
    void evaluer_doitRefuserSiEffectifInsuffisant() {
        Animatrice autre = new Animatrice(); autre.setId(2L);

        AbsenceConge absAutre = AbsenceConge.builder()
                .animatrice(autre).type(TypeAbsenceConge.CONGE_ANNUEL)
                .dateDebut(LocalDate.now().plusDays(9))
                .dateFin(LocalDate.now().plusDays(16))
                .nbJours(7).statut(StatutAbsenceConge.APPROUVE).build();
        absAutre.setId(88L);

        when(quotaCongeRepository.findByType(any())).thenReturn(Optional.of(quota));
        when(absenceCongeRepository.findByAnimatriceId(1L)).thenReturn(List.of());
        when(absenceCongeRepository.findAll()).thenReturn(List.of(absAutre));
        when(animatriceRepository.findByStatut(StatutAnimatrice.ACTIVE))
                .thenReturn(List.of(animatrice, autre));
        when(historiqueDecisionRepository.save(any())).thenReturn(null);

        ResultatEvaluationDTO r = moteurRegles.evaluer(demande);

        assertThat(r.getDecision()).isEqualTo("AUTO_REFUSE");
        assertThat(r.getRegleDeclenchee()).isEqualTo("EFFECTIF_MINIMUM");
    }

    // ===== Auto-approuvé =====
    @Test
    @DisplayName("evaluer — doit auto-approuver si toutes les règles passent")
    void evaluer_doitAutoApprouver() {
        Animatrice a2 = new Animatrice(); a2.setId(2L);
        Animatrice a3 = new Animatrice(); a3.setId(3L);
        Animatrice a4 = new Animatrice(); a4.setId(4L);

        when(quotaCongeRepository.findByType(any())).thenReturn(Optional.of(quota));
        when(absenceCongeRepository.findByAnimatriceId(1L)).thenReturn(List.of());
        when(absenceCongeRepository.findAll()).thenReturn(List.of());
        when(animatriceRepository.findByStatut(StatutAnimatrice.ACTIVE))
                .thenReturn(List.of(animatrice, a2, a3, a4));
        when(historiqueDecisionRepository.save(any())).thenReturn(null);

        ResultatEvaluationDTO r = moteurRegles.evaluer(demande);

        assertThat(r.getDecision()).isEqualTo("AUTO_APPROUVE");
        assertThat(r.getRegleDeclenchee()).isEqualTo("TOUTES_REGLES_OK");
    }

    // ===== TRANSMIS_ADMIN si autoApprobation=false =====
    @Test
    @DisplayName("evaluer — doit transmettre à l'admin si autoApprobation désactivée")
    void evaluer_doitTransmettreAdminSiAutoApprobationFalse() {
        quota = QuotaConge.builder()
                .type(TypeAbsenceConge.ABSENCE)
                .nbJoursMax(10)
                .delaiPrevenanceJours(1)
                .effectifMinimum(1)
                .autoApprobation(false) // pas d'auto-approbation
                .build();

        Animatrice a2 = new Animatrice(); a2.setId(2L);

        when(quotaCongeRepository.findByType(any())).thenReturn(Optional.of(quota));
        when(absenceCongeRepository.findByAnimatriceId(1L)).thenReturn(List.of());
        when(absenceCongeRepository.findAll()).thenReturn(List.of());
        when(animatriceRepository.findByStatut(StatutAnimatrice.ACTIVE))
                .thenReturn(List.of(animatrice, a2));
        when(historiqueDecisionRepository.save(any())).thenReturn(null);

        ResultatEvaluationDTO r = moteurRegles.evaluer(demande);

        assertThat(r.getDecision()).isEqualTo("TRANSMIS_ADMIN");
    }

    // ===== Quota par défaut si non trouvé =====
    @Test
    @DisplayName("evaluer — doit utiliser le quota par défaut si non configuré en BD")
    void evaluer_doitUtiliserQuotaParDefaut() {
        Animatrice a2 = new Animatrice(); a2.setId(2L);
        Animatrice a3 = new Animatrice(); a3.setId(3L);

        // Quota non trouvé → quota par défaut CONGE_ANNUEL : 30j, 7j préavis, effectif 2
        when(quotaCongeRepository.findByType(any())).thenReturn(Optional.empty());
        when(absenceCongeRepository.findByAnimatriceId(1L)).thenReturn(List.of());
        when(absenceCongeRepository.findAll()).thenReturn(List.of());
        when(animatriceRepository.findByStatut(StatutAnimatrice.ACTIVE))
                .thenReturn(List.of(animatrice, a2, a3));
        when(historiqueDecisionRepository.save(any())).thenReturn(null);

        ResultatEvaluationDTO r = moteurRegles.evaluer(demande);

        // Quota par défaut CONGE_ANNUEL = autoApprobation true → AUTO_APPROUVE
        assertThat(r.getDecision()).isEqualTo("AUTO_APPROUVE");
    }
}