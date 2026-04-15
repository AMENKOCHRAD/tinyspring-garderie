package com.tinyspring.garderie.service.RH;

import com.tinyspring.garderie.dto.RH.ResultatEvaluationDTO;
import com.tinyspring.garderie.entity.RH.AbsenceConge;
import com.tinyspring.garderie.entity.RH.HistoriqueDecision;
import com.tinyspring.garderie.entity.RH.QuotaConge;
import com.tinyspring.garderie.entity.RH.enums.StatutAbsenceConge;
import com.tinyspring.garderie.entity.RH.enums.StatutAnimatrice;
import com.tinyspring.garderie.entity.RH.enums.TypeAbsenceConge;
import com.tinyspring.garderie.repository.RH.AbsenceCongeRepository;
import com.tinyspring.garderie.repository.RH.AnimatriceRepository;
import com.tinyspring.garderie.repository.RH.HistoriqueDecisionRepository;
import com.tinyspring.garderie.repository.RH.QuotaCongeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MoteurReglesService {

    private final QuotaCongeRepository quotaCongeRepository;
    private final AbsenceCongeRepository absenceCongeRepository;
    private final HistoriqueDecisionRepository historiqueDecisionRepository;
    private final AnimatriceRepository animatriceRepository; // ✅ CORRIGÉ

    /**
     * Point d'entrée principal — évalue toutes les règles pour une demande
     */
    public ResultatEvaluationDTO evaluer(AbsenceConge demande) {

        // Charger le quota pour ce type de congé
        QuotaConge quota = quotaCongeRepository.findByType(demande.getType())
                .orElse(getQuotaParDefaut(demande.getType()));

        // ===== RÈGLE 1 : Délai de prévenance =====
        ResultatEvaluationDTO regle1 = verifierDelaiPrevenance(demande, quota);
        if ("AUTO_REFUSE".equals(regle1.getDecision())) {
            sauvegarderHistorique(demande, regle1);
            return regle1;
        }

        // ===== RÈGLE 2 : Quota annuel =====
        int joursDejaUtilises = calculerJoursUtilises(demande);
        int joursRestants = quota.getNbJoursMax() - joursDejaUtilises;
        ResultatEvaluationDTO regle2 = verifierQuota(demande, quota, joursDejaUtilises, joursRestants);
        if ("AUTO_REFUSE".equals(regle2.getDecision())) {
            sauvegarderHistorique(demande, regle2);
            return regle2;
        }

        // ===== RÈGLE 3 : Chevauchement avec autres absences =====
        ResultatEvaluationDTO regle3 = verifierChevauchement(demande);
        if ("AUTO_REFUSE".equals(regle3.getDecision())) {
            sauvegarderHistorique(demande, regle3);
            return regle3;
        }

        // ===== RÈGLE 4 : Effectif minimum =====
        ResultatEvaluationDTO regle4 = verifierEffectifMinimum(demande, quota);
        if ("AUTO_REFUSE".equals(regle4.getDecision())) {
            sauvegarderHistorique(demande, regle4);
            return regle4;
        }

        // ===== TOUTES LES RÈGLES PASSENT =====
        if (quota.isAutoApprobation()) {
            ResultatEvaluationDTO approuve = ResultatEvaluationDTO.builder()
                    .decision("AUTO_APPROUVE")
                    .regleDeclenchee("TOUTES_REGLES_OK")
                    .explication("✅ Toutes les règles ont été vérifiées automatiquement. " +
                            "Votre demande de " + demande.getNbJours() + " jour(s) est approuvée. " +
                            "Il vous reste " + (joursRestants - demande.getNbJours()) + " jour(s) cette année.")
                    .joursDejaUtilises(joursDejaUtilises)
                    .joursRestants(joursRestants - demande.getNbJours())
                    .quotaMax(quota.getNbJoursMax())
                    .build();
            sauvegarderHistorique(demande, approuve);
            return approuve;
        }

        // Auto-approbation désactivée → transmettre à l'admin
        ResultatEvaluationDTO transmis = ResultatEvaluationDTO.builder()
                .decision("TRANSMIS_ADMIN")
                .regleDeclenchee("TOUTES_REGLES_OK")
                .explication("⏳ Toutes les règles ont été vérifiées. " +
                        "Votre demande est transmise à l'administrateur pour validation finale.")
                .joursDejaUtilises(joursDejaUtilises)
                .joursRestants(joursRestants)
                .quotaMax(quota.getNbJoursMax())
                .build();
        sauvegarderHistorique(demande, transmis);
        return transmis;
    }

    // ===== RÈGLE 1 : Délai de prévenance =====
    private ResultatEvaluationDTO verifierDelaiPrevenance(AbsenceConge demande, QuotaConge quota) {
        long joursAvant = ChronoUnit.DAYS.between(LocalDate.now(), demande.getDateDebut());

        if (joursAvant < quota.getDelaiPrevenanceJours()) {
            return ResultatEvaluationDTO.builder()
                    .decision("AUTO_REFUSE")
                    .regleDeclenchee("DELAI_PREVENANCE")
                    .explication("❌ Délai de prévenance insuffisant. " +
                            "Pour un(e) " + formatType(demande.getType()) +
                            ", vous devez soumettre votre demande au moins " +
                            quota.getDelaiPrevenanceJours() + " jour(s) à l'avance. " +
                            "Vous avez soumis " + joursAvant + " jour(s) avant le début.")
                    .build();
        }

        return ResultatEvaluationDTO.builder().decision("OK").build();
    }

    // ===== RÈGLE 2 : Quota annuel =====
    private ResultatEvaluationDTO verifierQuota(AbsenceConge demande, QuotaConge quota,
                                                int joursDejaUtilises, int joursRestants) {
        if (demande.getNbJours() > joursRestants) {
            return ResultatEvaluationDTO.builder()
                    .decision("AUTO_REFUSE")
                    .regleDeclenchee("QUOTA_DEPASSE")
                    .explication("❌ Quota annuel dépassé. " +
                            "Pour le type " + formatType(demande.getType()) +
                            ", le maximum est de " + quota.getNbJoursMax() + " jour(s)/an. " +
                            "Vous avez déjà utilisé " + joursDejaUtilises + " jour(s). " +
                            "Il vous reste " + joursRestants + " jour(s), " +
                            "mais vous demandez " + demande.getNbJours() + " jour(s).")
                    .joursDejaUtilises(joursDejaUtilises)
                    .joursRestants(joursRestants)
                    .quotaMax(quota.getNbJoursMax())
                    .build();
        }

        return ResultatEvaluationDTO.builder().decision("OK").build();
    }

    // ===== RÈGLE 3 : Chevauchement =====
    private ResultatEvaluationDTO verifierChevauchement(AbsenceConge demande) {
        List<AbsenceConge> existantes = absenceCongeRepository.findByAnimatriceId(
                demande.getAnimatrice().getId());

        for (AbsenceConge existante : existantes) {
            if (StatutAbsenceConge.REFUSE.equals(existante.getStatut())) continue;
            if (existante.getId() != null && existante.getId().equals(demande.getId())) continue;

            boolean chevauchement =
                    !demande.getDateDebut().isAfter(existante.getDateFin()) &&
                            !demande.getDateFin().isBefore(existante.getDateDebut());

            if (chevauchement) {
                return ResultatEvaluationDTO.builder()
                        .decision("AUTO_REFUSE")
                        .regleDeclenchee("CHEVAUCHEMENT_DATES")
                        .explication("❌ Chevauchement détecté. " +
                                "Vous avez déjà une demande (" + existante.getStatut().name() + ") " +
                                "du " + existante.getDateDebut() + " au " + existante.getDateFin() +
                                " qui chevauche votre nouvelle demande.")
                        .build();
            }
        }

        return ResultatEvaluationDTO.builder().decision("OK").build();
    }

    // ===== RÈGLE 4 : Effectif minimum — CORRIGÉE =====
    private ResultatEvaluationDTO verifierEffectifMinimum(AbsenceConge demande, QuotaConge quota) {

        // ✅ Compter TOUTES les animatrices actives (pas seulement celles avec absences)
        long totalAnimatrices = animatriceRepository
                .findByStatut(StatutAnimatrice.ACTIVE).size();

        // Animatrices déjà absentes sur la même période
        long absencesEnParallele = absenceCongeRepository.findAll().stream()
                .filter(a -> !StatutAbsenceConge.REFUSE.equals(a.getStatut()))
                .filter(a -> !a.getAnimatrice().getId().equals(demande.getAnimatrice().getId()))
                .filter(a -> !a.getDateDebut().isAfter(demande.getDateFin()) &&
                        !a.getDateFin().isBefore(demande.getDateDebut()))
                .count();

        // Animatrices présentes si cette demande est approuvée
        long animatricesPresentes = totalAnimatrices - absencesEnParallele - 1;

        if (animatricesPresentes < quota.getEffectifMinimum()) {
            return ResultatEvaluationDTO.builder()
                    .decision("AUTO_REFUSE")
                    .regleDeclenchee("EFFECTIF_MINIMUM")
                    .explication("❌ Effectif minimum non respecté. " +
                            "Il faut au minimum " + quota.getEffectifMinimum() +
                            " animatrice(s) présente(s) sur cette période. " +
                            "Sur " + totalAnimatrices + " animatrice(s) active(s), " +
                            absencesEnParallele + " sont déjà absente(s) à ces dates.")
                    .build();
        }

        return ResultatEvaluationDTO.builder().decision("OK").build();
    }

    // ===== Calcul jours utilisés cette année =====
    private int calculerJoursUtilises(AbsenceConge demande) {
        int anneeEnCours = LocalDate.now().getYear();

        return absenceCongeRepository.findByAnimatriceId(demande.getAnimatrice().getId())
                .stream()
                .filter(a -> demande.getType().equals(a.getType()))
                .filter(a -> !StatutAbsenceConge.REFUSE.equals(a.getStatut()))
                .filter(a -> a.getDateDebut().getYear() == anneeEnCours)
                // ✅ Exclure la demande en cours elle-même
                .filter(a -> !a.getId().equals(demande.getId()))
                .mapToInt(a -> a.getNbJours() != null ? a.getNbJours() : 0)
                .sum();
    }

    // ===== Quota par défaut si non configuré =====
    private QuotaConge getQuotaParDefaut(TypeAbsenceConge type) {
        return switch (type) {
            case CONGE_ANNUEL    -> QuotaConge.builder()
                    .type(type).nbJoursMax(30)
                    .delaiPrevenanceJours(7).effectifMinimum(2)
                    .autoApprobation(true).build();
            case CONGE_MALADIE   -> QuotaConge.builder()
                    .type(type).nbJoursMax(15)
                    .delaiPrevenanceJours(0).effectifMinimum(1)
                    .autoApprobation(true).build();
            case CONGE_MATERNITE -> QuotaConge.builder()
                    .type(type).nbJoursMax(90)
                    .delaiPrevenanceJours(30).effectifMinimum(1)
                    .autoApprobation(true).build();
            case ABSENCE         -> QuotaConge.builder()
                    .type(type).nbJoursMax(10)
                    .delaiPrevenanceJours(1).effectifMinimum(2)
                    .autoApprobation(false).build();
        };
    }

    // ===== Sauvegarder l'historique =====
    private void sauvegarderHistorique(AbsenceConge demande, ResultatEvaluationDTO resultat) {
        if (demande.getId() == null) return;

        HistoriqueDecision historique = HistoriqueDecision.builder()
                .absenceConge(demande)
                .regleDeclenchee(resultat.getRegleDeclenchee())
                .decision(resultat.getDecision())
                .explication(resultat.getExplication())
                .build();

        historiqueDecisionRepository.save(historique);
    }

    // ===== Format lisible du type =====
    private String formatType(TypeAbsenceConge type) {
        return switch (type) {
            case CONGE_ANNUEL    -> "Congé Annuel";
            case CONGE_MALADIE   -> "Congé Maladie";
            case CONGE_MATERNITE -> "Congé Maternité";
            case ABSENCE         -> "Absence";
        };
    }
}