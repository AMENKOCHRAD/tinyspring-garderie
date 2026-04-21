package com.tinyspring.garderie.service.RH;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinyspring.garderie.dto.RH.DashboardStatsDTO;
import com.tinyspring.garderie.entity.RH.AbsenceConge;
import com.tinyspring.garderie.entity.RH.Animatrice;
import com.tinyspring.garderie.entity.RH.enums.StatutAbsenceConge;
import com.tinyspring.garderie.entity.RH.enums.TypeAbsenceConge;
import com.tinyspring.garderie.repository.RH.AbsenceCongeRepository;
import com.tinyspring.garderie.repository.RH.AnimatriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DatasetGeneratorService {

    private final AbsenceCongeRepository absenceCongeRepository;
    private final AnimatriceRepository animatriceRepository;
    private final DashboardService dashboardService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public String genererDataset() throws Exception {

        List<Map<String, String>> dataset = new ArrayList<>();

        List<Animatrice> animatrices = animatriceRepository.findAll();
        List<AbsenceConge> absences = absenceCongeRepository.findAll();
        DashboardStatsDTO stats = dashboardService.getStats();

        dataset.addAll(genererExemplesDashboard(stats));
        dataset.addAll(genererExemplesAnimatrices(animatrices, stats));
        dataset.addAll(genererExemplesAbsences(absences, stats, animatrices));
        dataset.addAll(genererExemplesQuotas(animatrices, absences));
        dataset.addAll(genererExemplesRapports(animatrices, absences, stats));
        dataset.addAll(genererExemplesMixtes(stats));

        String outputPath = "dataset/tinyspring_rh_dataset.json";
        new File("dataset").mkdirs();
        objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(new File(outputPath), dataset);

        System.out.println("✅ Dataset généré : " + dataset.size() + " exemples → " + outputPath);
        return "Dataset généré : " + dataset.size() + " exemples → " + outputPath;
    }

    private List<Map<String, String>> genererExemplesDashboard(DashboardStatsDTO stats) {
        List<Map<String, String>> ex = new ArrayList<>();
        String ctx = buildContexteDashboard(stats);

        ex.add(creer("Montre-moi le tableau de bord RH", ctx, buildReponseDashboard(stats)));

        ex.add(creer("Donne-moi un résumé rapide de la situation RH", ctx,
                "📊 RÉSUMÉ RH — TINYSPRING GARDERIE\n\n" +
                        "👥 " + stats.getTotalAnimatrices() + " animatrices (" + stats.getAnimatricesActives() + " actives)\n" +
                        "📋 " + stats.getTotalAbsences() + " demandes d'absences au total\n" +
                        "⏳ " + stats.getAbsencesEnAttente() + " en attente de traitement\n\n" +
                        (stats.getAbsencesEnAttente() > 0 ?
                                "⚠️ ACTION REQUISE : " + stats.getAbsencesEnAttente() + " demande(s) en attente !\n" : "") +
                        "✅ Taux d'approbation : " +
                        (stats.getTotalAbsences() > 0 ?
                                (stats.getAbsencesApprouvees() * 100 / stats.getTotalAbsences()) + "%" : "N/A")));

        ex.add(creer("Quels sont les chiffres clés RH ?", ctx,
                "🔢 CHIFFRES CLÉS RH\n\nANIMATRICES\n" +
                        "• Total : " + stats.getTotalAnimatrices() + "\n" +
                        "• Actives : " + stats.getAnimatricesActives() + "\n" +
                        "• Inactives : " + stats.getAnimatricesInactives() + "\n\n" +
                        "ABSENCES\n" +
                        "• Total : " + stats.getTotalAbsences() + "\n" +
                        "• Congés annuels : " + stats.getCongesAnnuels() + "\n" +
                        "• Congés maladie : " + stats.getCongesMaladie() + "\n" +
                        "• Congés maternité : " + stats.getCongesMaternite() + "\n" +
                        "• Absences : " + stats.getAbsences()));

        ex.add(creer("Y a-t-il des urgences RH à traiter ?", ctx, buildUrgencesRH(stats)));

        ex.add(creer("Quel est le taux d'approbation des congés ?", ctx,
                "📈 TAUX D'APPROBATION DES CONGÉS\n\n" +
                        "• Total demandes : " + stats.getTotalAbsences() + "\n" +
                        "• Approuvées : " + stats.getAbsencesApprouvees() + "\n" +
                        "• Refusées : " + stats.getAbsencesRefusees() + "\n" +
                        "• En attente : " + stats.getAbsencesEnAttente() + "\n\n" +
                        "📊 Taux d'approbation : " +
                        (stats.getTotalAbsences() > 0 ?
                                (stats.getAbsencesApprouvees() * 100 / stats.getTotalAbsences()) + "%" : "N/A") + "\n" +
                        "📊 Taux de refus : " +
                        (stats.getTotalAbsences() > 0 ?
                                (stats.getAbsencesRefusees() * 100 / stats.getTotalAbsences()) + "%" : "N/A")));

        return ex;
    }

    private List<Map<String, String>> genererExemplesAnimatrices(
            List<Animatrice> animatrices, DashboardStatsDTO stats) {
        List<Map<String, String>> ex = new ArrayList<>();
        String ctx = buildContexteAnimatrices(animatrices);

        ex.add(creer("Donne-moi la liste de toutes les animatrices", ctx,
                buildListeAnimatrices(animatrices)));

        ex.add(creer("Quel est l'état des animatrices actives et inactives ?", ctx,
                "👥 ÉTAT DES ANIMATRICES\n\n📊 Statistiques :\n" +
                        "• Total : " + stats.getTotalAnimatrices() + "\n" +
                        "• ✅ Actives : " + stats.getAnimatricesActives() + "\n" +
                        "• ⛔ Inactives : " + stats.getAnimatricesInactives() + "\n\n" +
                        "📈 Taux d'activité : " +
                        (stats.getTotalAnimatrices() > 0 ?
                                (stats.getAnimatricesActives() * 100 / stats.getTotalAnimatrices()) + "%" : "N/A")));

        animatrices.stream().limit(5).forEach(a -> {
            List<AbsenceConge> sesAbsences = absenceCongeRepository.findByAnimatriceId(a.getId());
            int joursUtilises = sesAbsences.stream()
                    .filter(ac -> !StatutAbsenceConge.REFUSE.equals(ac.getStatut()))
                    .mapToInt(ac -> ac.getNbJours() != null ? ac.getNbJours() : 0)
                    .sum();

            ex.add(creer("Quel est le bilan de " + a.getPrenom() + " " + a.getNom() + " ?",
                    "Animatrice : " + a.getPrenom() + " " + a.getNom() +
                            " | Statut : " + a.getStatut() +
                            " | Absences : " + sesAbsences.size(),
                    "👤 BILAN ANIMATRICE — " + a.getPrenom().toUpperCase() + " " + a.getNom().toUpperCase() + "\n\n" +
                            "📋 Informations :\n" +
                            "• Nom : " + a.getPrenom() + " " + a.getNom() + "\n" +
                            "• Statut : " + a.getStatut() + "\n" +
                            "• Spécialité : " + (a.getSpecialite() != null ? a.getSpecialite() : "Non renseignée") + "\n" +
                            "• Date embauche : " + (a.getDateEmbauche() != null ? a.getDateEmbauche().format(fmt) : "Non renseignée") + "\n\n" +
                            "📊 Absences :\n" +
                            "• Total demandes : " + sesAbsences.size() + "\n" +
                            "• Approuvées : " + sesAbsences.stream()
                            .filter(ac -> StatutAbsenceConge.APPROUVE.equals(ac.getStatut())).count() + "\n" +
                            "• Refusées : " + sesAbsences.stream()
                            .filter(ac -> StatutAbsenceConge.REFUSE.equals(ac.getStatut())).count() + "\n" +
                            "• Jours utilisés : " + joursUtilises));
        });

        return ex;
    }

    private List<Map<String, String>> genererExemplesAbsences(
            List<AbsenceConge> absences, DashboardStatsDTO stats, List<Animatrice> animatrices) {
        List<Map<String, String>> ex = new ArrayList<>();
        String ctx = buildContexteAbsences(absences, stats);
        LocalDate maintenant = LocalDate.now();

        ex.add(creer("Génère le rapport mensuel des absences",
                ctx, buildRapportMensuel(absences, stats, maintenant)));

        ex.add(creer("Liste toutes les demandes en attente avec recommandations",
                ctx, buildDemandesEnAttente(absences)));

        ex.add(creer("Analyse les absences par type",
                ctx, buildAnalyseParType(stats)));

        ex.add(creer("Quelles sont les absences approuvées ce mois ?",
                ctx, buildAbsencesApprouvees(absences, maintenant)));

        ex.add(creer("Quelles sont les absences refusées et pourquoi ?",
                ctx, buildAbsencesRefusees(absences)));

        ex.add(creer("Génère un rapport d'alerte sur les absences critiques",
                ctx, buildAlertesAbsences(absences, stats, animatrices)));

        for (TypeAbsenceConge type : TypeAbsenceConge.values()) {
            List<AbsenceConge> parType = absences.stream()
                    .filter(a -> type.equals(a.getType()))
                    .collect(Collectors.toList());
            int totalJours = parType.stream()
                    .filter(a -> !StatutAbsenceConge.REFUSE.equals(a.getStatut()))
                    .mapToInt(a -> a.getNbJours() != null ? a.getNbJours() : 0)
                    .sum();

            ex.add(creer("Combien de " + type.name().replace("_", " ").toLowerCase() + " ont été demandés ?",
                    ctx,
                    "📋 ANALYSE " + type.name().replace("_", " ") + "\n\n" +
                            "• Demandes totales : " + parType.size() + "\n" +
                            "• Approuvées : " + parType.stream()
                            .filter(a -> StatutAbsenceConge.APPROUVE.equals(a.getStatut())).count() + "\n" +
                            "• Refusées : " + parType.stream()
                            .filter(a -> StatutAbsenceConge.REFUSE.equals(a.getStatut())).count() + "\n" +
                            "• Total jours accordés : " + totalJours));
        }

        return ex;
    }

    private List<Map<String, String>> genererExemplesQuotas(
            List<Animatrice> animatrices, List<AbsenceConge> absences) {
        List<Map<String, String>> ex = new ArrayList<>();
        String ctx = "Quotas TinySpring : Congé annuel 30j, Maladie 15j, Maternité 90j, Absence 10j";

        ex.add(creer("Quelles animatrices ont dépassé leur quota ?", ctx,
                buildAlertesQuotas(animatrices)));

        ex.add(creer("Analyse les décisions automatiques du moteur de règles",
                buildContexteAbsences(absences, null),
                buildAnalyseMoteur(absences)));

        ex.add(creer("Comment fonctionne le moteur de règles ?",
                "Moteur de règles TinySpring configuré",
                "🤖 MOTEUR DE RÈGLES — FONCTIONNEMENT\n\n" +
                        "1️⃣ DÉLAI DE PRÉVENANCE\n2️⃣ QUOTA ANNUEL\n3️⃣ CHEVAUCHEMENT\n4️⃣ EFFECTIF MINIMUM\n\n" +
                        "✅ Toutes OK → AUTO-APPROUVÉ\n❌ Règle échoue → AUTO-REFUSÉ\n⏳ Auto-approbation off → TRANSMIS ADMIN"));

        return ex;
    }

    private List<Map<String, String>> genererExemplesRapports(
            List<Animatrice> animatrices, List<AbsenceConge> absences, DashboardStatsDTO stats) {
        List<Map<String, String>> ex = new ArrayList<>();
        String ctx = buildContexteComplet(stats);
        LocalDate maintenant = LocalDate.now();

        ex.add(creer("Génère un bilan RH complet", ctx, buildBilanComplet(stats)));

        ex.add(creer("Génère le rapport mensuel de " + maintenant.getMonth().name(), ctx,
                buildRapportMensuel(absences, stats, maintenant)));

        ex.add(creer("Génère le rapport trimestriel RH", ctx,
                "📊 RAPPORT TRIMESTRIEL RH — TINYSPRING GARDERIE\n\n" +
                        buildBilanComplet(stats) + "\n\n📅 Période : Trimestre " + maintenant.getYear()));

        return ex;
    }

    private List<Map<String, String>> genererExemplesMixtes(DashboardStatsDTO stats) {
        List<Map<String, String>> ex = new ArrayList<>();
        String ctx = buildContexteDashboard(stats);

        ex.add(creer("Donne-moi des recommandations pour améliorer la gestion RH", ctx,
                "💡 RECOMMANDATIONS RH — TINYSPRING GARDERIE\n\n" +
                        "1. GESTION DES ABSENCES\n" +
                        (stats.getAbsencesEnAttente() > 0 ?
                                "• ⚠️ Traiter " + stats.getAbsencesEnAttente() + " demande(s) en attente\n" : "") +
                        "• Activer l'auto-approbation pour réduire la charge admin\n\n" +
                        "2. GESTION DES ANIMATRICES\n" +
                        "• Maintenir un effectif minimum de 2 par groupe\n\n" +
                        "3. MOTEUR DE RÈGLES\n" +
                        "• Configurer les quotas selon les besoins"));

        ex.add(creer("Prépare un rapport pour une réunion de direction", ctx,
                "📋 RAPPORT DIRECTION — TINYSPRING GARDERIE\n\n" +
                        "Date : " + LocalDate.now().format(fmt) + "\n\n" +
                        "1. EFFECTIF\n   • " + stats.getTotalAnimatrices() + " animatrices\n\n" +
                        "2. ABSENCES\n   • " + stats.getTotalAbsences() + " demandes\n" +
                        "   • En attente : " + stats.getAbsencesEnAttente()));

        return ex;
    }

    // ===== BUILDERS =====

    private String buildContexteDashboard(DashboardStatsDTO stats) {
        return "Dashboard TinySpring :\n" +
                "Animatrices: total=" + stats.getTotalAnimatrices() +
                " actives=" + stats.getAnimatricesActives() +
                " inactives=" + stats.getAnimatricesInactives() + "\n" +
                "Absences: total=" + stats.getTotalAbsences() +
                " approuvées=" + stats.getAbsencesApprouvees() +
                " refusées=" + stats.getAbsencesRefusees() +
                " enAttente=" + stats.getAbsencesEnAttente() + "\n" +
                "Types: congesAnnuels=" + stats.getCongesAnnuels() +
                " maladie=" + stats.getCongesMaladie() +
                " maternite=" + stats.getCongesMaternite() +
                " absences=" + stats.getAbsences();
    }

    private String buildReponseDashboard(DashboardStatsDTO stats) {
        return "📊 TABLEAU DE BORD RH — TINYSPRING GARDERIE\n\n" +
                "👥 ANIMATRICES\n" +
                "• Total : " + stats.getTotalAnimatrices() + "\n" +
                "• ✅ Actives : " + stats.getAnimatricesActives() + "\n" +
                "• ⛔ Inactives : " + stats.getAnimatricesInactives() + "\n\n" +
                "📋 ABSENCES & CONGÉS\n" +
                "• Total demandes : " + stats.getTotalAbsences() + "\n" +
                "• ✅ Approuvées : " + stats.getAbsencesApprouvees() + "\n" +
                "• ❌ Refusées : " + stats.getAbsencesRefusees() + "\n" +
                "• ⏳ En attente : " + stats.getAbsencesEnAttente() + "\n\n" +
                buildUrgencesRH(stats);
    }

    private String buildUrgencesRH(DashboardStatsDTO stats) {
        StringBuilder sb = new StringBuilder("🚨 URGENCES\n");
        boolean urgent = false;
        if (stats.getAbsencesEnAttente() > 0) {
            sb.append("• ⚠️ ").append(stats.getAbsencesEnAttente()).append(" demande(s) en attente\n");
            urgent = true;
        }
        if (stats.getAnimatricesInactives() > 0) {
            sb.append("• ⚠️ ").append(stats.getAnimatricesInactives()).append(" animatrice(s) inactive(s)\n");
            urgent = true;
        }
        if (!urgent) sb.append("• ✅ Aucune urgence détectée\n");
        return sb.toString();
    }

    private String buildContexteAnimatrices(List<Animatrice> animatrices) {
        StringBuilder sb = new StringBuilder("Animatrices TinySpring :\n");
        animatrices.forEach(a -> sb.append("• ").append(a.getPrenom()).append(" ")
                .append(a.getNom()).append(" | ").append(a.getStatut()).append("\n"));
        return sb.toString();
    }

    private String buildListeAnimatrices(List<Animatrice> animatrices) {
        StringBuilder sb = new StringBuilder("👥 LISTE DES ANIMATRICES\n\n");
        animatrices.forEach(a -> sb.append("• ")
                .append(a.getPrenom()).append(" ").append(a.getNom())
                .append(" — ").append(a.getStatut())
                .append(a.getSpecialite() != null ? " — " + a.getSpecialite() : "")
                .append("\n"));
        return sb.toString();
    }

    private String buildContexteAbsences(List<AbsenceConge> absences, DashboardStatsDTO stats) {
        StringBuilder sb = new StringBuilder("Absences TinySpring — Total : ")
                .append(absences.size()).append("\n");
        absences.stream().limit(10).forEach(a -> sb.append("• ")
                .append(a.getAnimatrice().getPrenom()).append(" ")
                .append(a.getAnimatrice().getNom())
                .append(" | ").append(a.getType())
                .append(" | ").append(a.getStatut())
                .append(" | ").append(a.getNbJours()).append("j\n"));
        return sb.toString();
    }

    private String buildRapportMensuel(List<AbsenceConge> absences,
                                       DashboardStatsDTO stats, LocalDate maintenant) {
        long absMois = absences.stream()
                .filter(a -> a.getDateDebut().getMonthValue() == maintenant.getMonthValue()
                        && a.getDateDebut().getYear() == maintenant.getYear())
                .count();
        return "📅 RAPPORT MENSUEL — " + maintenant.getMonth().name() + " " + maintenant.getYear() + "\n\n" +
                "📊 Absences ce mois : " + absMois + "\n" +
                "✅ Approuvées : " + (stats != null ? stats.getAbsencesApprouvees() : "N/A") + "\n" +
                "❌ Refusées : " + (stats != null ? stats.getAbsencesRefusees() : "N/A") + "\n" +
                "⏳ En attente : " + (stats != null ? stats.getAbsencesEnAttente() : "N/A") + "\n\n" +
                buildAnalyseParType(stats);
    }

    private String buildDemandesEnAttente(List<AbsenceConge> absences) {
        StringBuilder sb = new StringBuilder("⏳ DEMANDES EN ATTENTE\n\n");
        List<AbsenceConge> enAttente = absences.stream()
                .filter(a -> StatutAbsenceConge.EN_ATTENTE.equals(a.getStatut()))
                .collect(Collectors.toList());
        if (enAttente.isEmpty()) {
            sb.append("✅ Aucune demande en attente — Excellent !\n");
        } else {
            enAttente.forEach(a -> sb.append("• ")
                    .append(a.getAnimatrice().getPrenom()).append(" ")
                    .append(a.getAnimatrice().getNom())
                    .append(" | ").append(a.getType().name().replace("_", " "))
                    .append(" | ").append(a.getNbJours()).append(" jours\n"));
        }
        return sb.toString();
    }

    private String buildAnalyseParType(DashboardStatsDTO stats) {
        if (stats == null) return "";
        return "📊 PAR TYPE D'ABSENCE\n" +
                "• Congés annuels : " + stats.getCongesAnnuels() + "\n" +
                "• Congés maladie : " + stats.getCongesMaladie() + "\n" +
                "• Congés maternité : " + stats.getCongesMaternite() + "\n" +
                "• Absences : " + stats.getAbsences();
    }

    private String buildAbsencesApprouvees(List<AbsenceConge> absences, LocalDate maintenant) {
        StringBuilder sb = new StringBuilder("✅ ABSENCES APPROUVÉES — " + maintenant.getMonth().name() + "\n\n");
        absences.stream()
                .filter(a -> StatutAbsenceConge.APPROUVE.equals(a.getStatut()))
                .filter(a -> a.getDateDebut().getMonthValue() == maintenant.getMonthValue())
                .forEach(a -> sb.append("• ")
                        .append(a.getAnimatrice().getPrenom()).append(" ").append(a.getAnimatrice().getNom())
                        .append(" | ").append(a.getType().name().replace("_", " "))
                        .append(" | ").append(a.getNbJours()).append(" jours\n"));
        return sb.toString();
    }

    private String buildAbsencesRefusees(List<AbsenceConge> absences) {
        StringBuilder sb = new StringBuilder("❌ ABSENCES REFUSÉES\n\n");
        List<AbsenceConge> refusees = absences.stream()
                .filter(a -> StatutAbsenceConge.REFUSE.equals(a.getStatut()))
                .collect(Collectors.toList());
        if (refusees.isEmpty()) {
            sb.append("✅ Aucune demande refusée.\n");
        } else {
            refusees.forEach(a -> sb.append("• ")
                    .append(a.getAnimatrice().getPrenom()).append(" ").append(a.getAnimatrice().getNom())
                    .append(" | ").append(a.getType().name().replace("_", " "))
                    .append(" | ").append(a.getNbJours()).append(" jours")
                    .append(a.getMotifDecision() != null ?
                            "\n  Motif : " + a.getMotifDecision().substring(0, Math.min(80, a.getMotifDecision().length())) : "")
                    .append("\n"));
        }
        return sb.toString();
    }

    private String buildAlertesAbsences(List<AbsenceConge> absences,
                                        DashboardStatsDTO stats, List<Animatrice> animatrices) {
        return "🚨 ALERTES ABSENCES\n\n" +
                (stats.getAbsencesEnAttente() > 0 ?
                        "⚠️ " + stats.getAbsencesEnAttente() + " demande(s) en attente\n" : "") +
                buildAlertesQuotas(animatrices) + "\n" +
                "💡 Traiter les demandes urgentes et vérifier les quotas";
    }

    private String buildAlertesQuotas(List<Animatrice> animatrices) {
        StringBuilder sb = new StringBuilder();
        boolean alerteTrouvee = false;
        for (Animatrice a : animatrices) {
            int joursUtilises = absenceCongeRepository.findByAnimatriceId(a.getId()).stream()
                    .filter(ac -> TypeAbsenceConge.CONGE_ANNUEL.equals(ac.getType()))
                    .filter(ac -> !StatutAbsenceConge.REFUSE.equals(ac.getStatut()))
                    .filter(ac -> ac.getDateDebut().getYear() == LocalDate.now().getYear())
                    .mapToInt(ac -> ac.getNbJours() != null ? ac.getNbJours() : 0)
                    .sum();
            if (joursUtilises >= 25) {
                alerteTrouvee = true;
                sb.append("⚠️ ").append(a.getPrenom()).append(" ").append(a.getNom())
                        .append(" : ").append(joursUtilises).append("/30j");
                if (joursUtilises >= 30) sb.append(" ❌ QUOTA DÉPASSÉ");
                sb.append("\n");
            }
        }
        if (!alerteTrouvee) sb.append("✅ Aucun dépassement de quota.\n");
        return sb.toString();
    }

    private String buildAnalyseMoteur(List<AbsenceConge> absences) {
        long autoApprouvees = absences.stream()
                .filter(a -> StatutAbsenceConge.APPROUVE.equals(a.getStatut()))
                .filter(a -> Boolean.TRUE.equals(a.getDecisionAutomatique())).count();
        long autoRefusees = absences.stream()
                .filter(a -> StatutAbsenceConge.REFUSE.equals(a.getStatut()))
                .filter(a -> Boolean.TRUE.equals(a.getDecisionAutomatique())).count();
        return "🤖 ANALYSE MOTEUR\n\n" +
                "• Auto-approuvées : " + autoApprouvees + "\n" +
                "• Auto-refusées : " + autoRefusees + "\n" +
                "• Taux d'automatisation : " +
                (absences.size() > 0 ? ((autoApprouvees + autoRefusees) * 100 / absences.size()) + "%" : "N/A");
    }

    private String buildContexteComplet(DashboardStatsDTO stats) {
        return "TinySpring — Animatrices: " + stats.getTotalAnimatrices() +
                " | Absences: " + stats.getTotalAbsences() +
                " | EnAttente: " + stats.getAbsencesEnAttente();
    }

    private String buildBilanComplet(DashboardStatsDTO stats) {
        return "📊 BILAN RH COMPLET — TINYSPRING GARDERIE\n\n" +
                "👥 EFFECTIF\n" +
                "• Total : " + stats.getTotalAnimatrices() + "\n" +
                "• Actives : " + stats.getAnimatricesActives() + "\n\n" +
                "📋 ABSENCES\n" +
                "• Total : " + stats.getTotalAbsences() + "\n" +
                "• Approuvées : " + stats.getAbsencesApprouvees() + "\n" +
                "• Refusées : " + stats.getAbsencesRefusees() + "\n" +
                "• En attente : " + stats.getAbsencesEnAttente() + "\n\n" +
                "💡 RECOMMANDATIONS\n" +
                (stats.getAbsencesEnAttente() > 0 ?
                        "• Traiter " + stats.getAbsencesEnAttente() + " demande(s)\n" : "") +
                "• Maintenir l'effectif minimum";
    }

    private Map<String, String> creer(String instruction, String input, String output) {
        Map<String, String> ex = new LinkedHashMap<>();
        ex.put("instruction", instruction);
        ex.put("input", input);
        ex.put("output", output);
        return ex;
    }
}