package com.tinyspring.garderie.service.RH;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinyspring.garderie.dto.RH.DashboardStatsDTO;
import com.tinyspring.garderie.entity.RH.AbsenceConge;
import com.tinyspring.garderie.entity.RH.Animatrice;
import com.tinyspring.garderie.entity.RH.Formation;
import com.tinyspring.garderie.entity.RH.enums.StatutAbsenceConge;
import com.tinyspring.garderie.entity.RH.enums.StatutAnimatrice;
import com.tinyspring.garderie.entity.RH.enums.StatutFormation;
import com.tinyspring.garderie.entity.RH.enums.TypeAbsenceConge;
import com.tinyspring.garderie.repository.RH.AbsenceCongeRepository;
import com.tinyspring.garderie.repository.RH.AnimatriceRepository;
import com.tinyspring.garderie.repository.RH.FormationRepository;
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
    private final FormationRepository formationRepository;
    private final DashboardService dashboardService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public String genererDataset() throws Exception {

        List<Map<String, String>> dataset = new ArrayList<>();

        List<Animatrice> animatrices = animatriceRepository.findAll();
        List<AbsenceConge> absences = absenceCongeRepository.findAll();
        List<Formation> formations = formationRepository.findAll();
        DashboardStatsDTO stats = dashboardService.getStats();

        dataset.addAll(genererExemplesDashboard(stats));
        dataset.addAll(genererExemplesAnimatrices(animatrices, stats));
        // ✅ animatrices passé en paramètre
        dataset.addAll(genererExemplesAbsences(absences, stats, animatrices));
        dataset.addAll(genererExemplesFormations(formations, stats));
        dataset.addAll(genererExemplesQuotas(animatrices, absences));
        dataset.addAll(genererExemplesRapports(animatrices, absences, formations, stats));
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
                        "⏳ " + stats.getAbsencesEnAttente() + " en attente de traitement\n" +
                        "📚 " + stats.getTotalFormations() + " formations\n\n" +
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
                        "• Absences : " + stats.getAbsences() + "\n\nFORMATIONS\n" +
                        "• Total : " + stats.getTotalFormations() + "\n" +
                        "• En cours : " + stats.getFormationsEnCours() + "\n" +
                        "• Terminées : " + stats.getFormationsTerminees()));

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
                                (stats.getAbsencesRefusees() * 100 / stats.getTotalAbsences()) + "%" : "N/A") + "\n\n" +
                        "💡 Recommandations :\n" +
                        (stats.getAbsencesEnAttente() > 0 ?
                                "• Traiter les " + stats.getAbsencesEnAttente() + " demandes en attente\n" : "") +
                        "• Analyser les motifs de refus pour améliorer le processus"));

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
                                (stats.getAnimatricesActives() * 100 / stats.getTotalAnimatrices()) + "%" : "N/A") + "\n\n" +
                        "💡 Recommandations :\n" +
                        (stats.getAnimatricesInactives() > 0 ?
                                "• Examiner le statut des " + stats.getAnimatricesInactives() + " animatrices inactives\n" : "") +
                        "• Maintenir un effectif minimum de 2 animatrices actives par groupe"));

        ex.add(creer("Quelles sont les animatrices récemment ajoutées ?", ctx,
                buildDernieresAnimatrices(stats)));

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
                            "• Jours utilisés : " + joursUtilises + "\n\n" +
                            (joursUtilises >= 25 ? "⚠️ Attention : quota proche ou dépassé !\n" : "") +
                            "💡 Recommandations :\n" +
                            (sesAbsences.stream().anyMatch(ac -> StatutAbsenceConge.EN_ATTENTE.equals(ac.getStatut())) ?
                                    "• Traiter les demandes en attente\n" : "") +
                            "• Surveiller le quota restant"));
        });

        return ex;
    }

    // ✅ CORRIGÉ — animatrices ajouté en paramètre
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

        ex.add(creer("Combien de jours d'absence ont été accordés au total ?",
                ctx,
                "📅 TOTAL JOURS D'ABSENCE ACCORDÉS\n\n" +
                        buildJoursParType(absences) +
                        "\n💡 Note : seules les absences approuvées sont comptabilisées."));

        // ✅ animatrices disponible ici via le paramètre
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
            String label = type.name().replace("_", " ").toLowerCase();

            ex.add(creer("Combien de " + label + " ont été demandés ?",
                    ctx,
                    "📋 ANALYSE " + type.name().replace("_", " ") + "\n\n" +
                            "• Demandes totales : " + parType.size() + "\n" +
                            "• Approuvées : " + parType.stream()
                            .filter(a -> StatutAbsenceConge.APPROUVE.equals(a.getStatut())).count() + "\n" +
                            "• Refusées : " + parType.stream()
                            .filter(a -> StatutAbsenceConge.REFUSE.equals(a.getStatut())).count() + "\n" +
                            "• En attente : " + parType.stream()
                            .filter(a -> StatutAbsenceConge.EN_ATTENTE.equals(a.getStatut())).count() + "\n" +
                            "• Total jours accordés : " + totalJours + "\n" +
                            "• Moyenne : " + (parType.size() > 0 ? totalJours / parType.size() : 0) + " jours/demande\n\n" +
                            "💡 Recommandations :\n" +
                            "• Surveiller les quotas pour ce type d'absence"));
        }

        return ex;
    }

    private List<Map<String, String>> genererExemplesFormations(
            List<Formation> formations, DashboardStatsDTO stats) {
        List<Map<String, String>> ex = new ArrayList<>();
        String ctx = buildContexteFormations(formations, stats);

        ex.add(creer("Quel est l'état des formations ?", ctx, buildEtatFormations(formations, stats)));
        ex.add(creer("Quelles formations sont en cours ?", ctx,
                buildFormationsParStatut(formations, StatutFormation.EN_COURS, "EN COURS")));
        ex.add(creer("Quelles formations sont disponibles pour inscription ?", ctx,
                buildFormationsParStatut(formations, StatutFormation.INSCRITE, "DISPONIBLES")));
        ex.add(creer("Quelles formations sont terminées ?", ctx,
                buildFormationsParStatut(formations, StatutFormation.TERMINEE, "TERMINÉES")));
        ex.add(creer("Génère un rapport complet sur les formations", ctx,
                "📚 RAPPORT FORMATIONS — TINYSPRING GARDERIE\n\n" +
                        "📊 Vue d'ensemble :\n" +
                        "• Total formations : " + stats.getTotalFormations() + "\n" +
                        "• Inscrites : " + stats.getFormationsInscrites() + "\n" +
                        "• En cours : " + stats.getFormationsEnCours() + "\n" +
                        "• Terminées : " + stats.getFormationsTerminees() + "\n\n" +
                        buildEtatFormations(formations, stats) + "\n\n" +
                        "💡 Recommandations :\n" +
                        "• Encourager les animatrices à s'inscrire aux formations disponibles\n" +
                        "• Planifier de nouvelles formations selon les besoins\n" +
                        "• Suivre les certifications obtenues"));

        formations.stream().limit(5).forEach(f ->
                ex.add(creer("Donne-moi les détails de la formation " + f.getTitre(),
                        "Formation : " + f.getTitre() + " | Statut : " + f.getStatutInscription(),
                        "📚 DÉTAIL FORMATION\n\n" +
                                "• Titre : " + f.getTitre() + "\n" +
                                "• Type : " + f.getType() + "\n" +
                                "• Formateur : " + (f.getFormateur() != null ? f.getFormateur() : "Non renseigné") + "\n" +
                                "• Statut : " + f.getStatutInscription() + "\n" +
                                "• Places max : " + (f.getPlacesMax() != null ? f.getPlacesMax() : "N/A") + "\n" +
                                "• Inscrits : " + (f.getAnimatrices() != null ? f.getAnimatrices().size() : 0) + "\n" +
                                "• Date début : " + (f.getDateDebut() != null ? f.getDateDebut().format(fmt) : "N/A") + "\n" +
                                "• Date fin : " + (f.getDateFin() != null ? f.getDateFin().format(fmt) : "N/A"))));

        return ex;
    }

    private List<Map<String, String>> genererExemplesQuotas(
            List<Animatrice> animatrices, List<AbsenceConge> absences) {
        List<Map<String, String>> ex = new ArrayList<>();
        String ctx = "Quotas TinySpring : Congé annuel 30j, Maladie 15j, Maternité 90j, Absence 10j";

        ex.add(creer("Quelles animatrices ont dépassé leur quota ?", ctx,
                buildAlertesQuotas(animatrices)));

        ex.add(creer("Génère un rapport d'alerte sur les quotas", ctx,
                "⚠️ RAPPORT ALERTES QUOTAS\n\n" + buildAlertesQuotas(animatrices) +
                        "\n💡 Actions recommandées :\n" +
                        "• Vérifier les quotas avant toute approbation\n" +
                        "• Informer les animatrices proches du quota\n" +
                        "• Planifier les congés pour éviter les dépassements"));

        ex.add(creer("Analyse les décisions automatiques du moteur de règles",
                buildContexteAbsences(absences, null),
                buildAnalyseMoteur(absences)));

        ex.add(creer("Comment fonctionne le moteur de règles ?",
                "Moteur de règles TinySpring configuré",
                "🤖 MOTEUR DE RÈGLES — FONCTIONNEMENT\n\n" +
                        "Le moteur évalue automatiquement chaque demande selon 4 règles :\n\n" +
                        "1️⃣ DÉLAI DE PRÉVENANCE\n" +
                        "• Congé annuel : 7 jours minimum\n" +
                        "• Congé maladie : 0 jour (urgent)\n" +
                        "• Congé maternité : 30 jours\n" +
                        "• Absence : 1 jour\n\n" +
                        "2️⃣ QUOTA ANNUEL\n" +
                        "• Congé annuel : 30j/an\n" +
                        "• Congé maladie : 15j/an\n" +
                        "• Congé maternité : 90j/an\n" +
                        "• Absence : 10j/an\n\n" +
                        "3️⃣ CHEVAUCHEMENT\n" +
                        "• Pas de double absence sur les mêmes dates\n\n" +
                        "4️⃣ EFFECTIF MINIMUM\n" +
                        "• Minimum 2 animatrices présentes par période\n\n" +
                        "✅ Si toutes les règles passent → AUTO-APPROUVÉ\n" +
                        "❌ Si une règle échoue → AUTO-REFUSÉ avec explication\n" +
                        "⏳ Si auto-approbation désactivée → TRANSMIS ADMIN"));

        return ex;
    }

    private List<Map<String, String>> genererExemplesRapports(
            List<Animatrice> animatrices, List<AbsenceConge> absences,
            List<Formation> formations, DashboardStatsDTO stats) {
        List<Map<String, String>> ex = new ArrayList<>();
        String ctx = buildContexteComplet(stats);
        LocalDate maintenant = LocalDate.now();

        ex.add(creer("Génère un bilan RH complet", ctx, buildBilanComplet(stats)));

        ex.add(creer("Génère le rapport mensuel de " + maintenant.getMonth().name(), ctx,
                buildRapportMensuel(absences, stats, maintenant)));

        ex.add(creer("Génère le rapport trimestriel RH", ctx,
                "📊 RAPPORT TRIMESTRIEL RH — TINYSPRING GARDERIE\n\n" +
                        buildBilanComplet(stats) + "\n\n" +
                        "📅 Période : Trimestre " + maintenant.getYear()));

        ex.add(creer("Génère le rapport annuel RH", ctx,
                "📈 RAPPORT ANNUEL RH — TINYSPRING GARDERIE " + maintenant.getYear() + "\n\n" +
                        buildBilanComplet(stats) + "\n\n" +
                        "📊 Bilan formations :\n" +
                        "• Total formations : " + stats.getTotalFormations() + "\n" +
                        "• Terminées : " + stats.getFormationsTerminees() + "\n\n" +
                        "🎯 Objectifs " + (maintenant.getYear() + 1) + " :\n" +
                        "• Maintenir le taux d'approbation au-dessus de 80%\n" +
                        "• Organiser au moins 5 formations\n" +
                        "• Recruter si effectif < 5 animatrices actives"));

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
                        "• Activer l'auto-approbation pour réduire la charge admin\n" +
                        "• Surveiller les quotas avant les périodes de vacances\n\n" +
                        "2. GESTION DES ANIMATRICES\n" +
                        (stats.getAnimatricesInactives() > 0 ?
                                "• Examiner le statut de " + stats.getAnimatricesInactives() + " animatrice(s) inactive(s)\n" : "") +
                        "• Maintenir un effectif minimum de 2 par groupe\n\n" +
                        "3. FORMATIONS\n" +
                        "• Planifier des formations régulières\n" +
                        "• Encourager les inscriptions\n\n" +
                        "4. MOTEUR DE RÈGLES\n" +
                        "• Configurer les quotas selon les besoins\n" +
                        "• Activer l'auto-approbation pour les types de congés fiables"));

        ex.add(creer("Quel est l'état général de la garderie TinySpring ?", ctx,
                "🌸 ÉTAT GÉNÉRAL — TINYSPRING GARDERIE\n\n" +
                        "✅ POINTS POSITIFS\n" +
                        (stats.getAnimatricesActives() >= 5 ? "• Bon effectif : " + stats.getAnimatricesActives() + " animatrices actives\n" : "") +
                        (stats.getAbsencesEnAttente() == 0 ? "• Aucune demande en attente\n" : "") +
                        (stats.getFormationsEnCours() > 0 ? "• " + stats.getFormationsEnCours() + " formation(s) en cours\n" : "") +
                        "\n⚠️ POINTS D'ATTENTION\n" +
                        (stats.getAbsencesEnAttente() > 0 ? "• " + stats.getAbsencesEnAttente() + " demande(s) en attente\n" : "") +
                        (stats.getAnimatricesInactives() > 0 ? "• " + stats.getAnimatricesInactives() + " animatrice(s) inactive(s)\n" : "") +
                        "\n💡 ACTIONS PRIORITAIRES\n" +
                        (stats.getAbsencesEnAttente() > 0 ? "• Traiter les demandes en attente\n" : "") +
                        "• Planifier les congés de l'équipe\n" +
                        "• Suivre les formations en cours"));

        ex.add(creer("Prépare un rapport pour une réunion de direction", ctx,
                "📋 RAPPORT DIRECTION — TINYSPRING GARDERIE\n\n" +
                        "Date : " + LocalDate.now().format(fmt) + "\n\n" +
                        "1. EFFECTIF\n" +
                        "   • " + stats.getTotalAnimatrices() + " animatrices (" +
                        stats.getAnimatricesActives() + " actives, " +
                        stats.getAnimatricesInactives() + " inactives)\n\n" +
                        "2. ABSENCES & CONGÉS\n" +
                        "   • " + stats.getTotalAbsences() + " demandes traitées\n" +
                        "   • Taux d'approbation : " +
                        (stats.getTotalAbsences() > 0 ?
                                (stats.getAbsencesApprouvees() * 100 / stats.getTotalAbsences()) + "%" : "N/A") + "\n" +
                        "   • En attente : " + stats.getAbsencesEnAttente() + "\n\n" +
                        "3. FORMATIONS\n" +
                        "   • " + stats.getTotalFormations() + " formations organisées\n" +
                        "   • " + stats.getFormationsTerminees() + " terminées\n\n" +
                        "4. DÉCISIONS REQUISES\n" +
                        (stats.getAbsencesEnAttente() > 0 ?
                                "   • Valider " + stats.getAbsencesEnAttente() + " demande(s) d'absence\n" : "") +
                        "   • Planification des formations à venir"));

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
                " absences=" + stats.getAbsences() + "\n" +
                "Formations: total=" + stats.getTotalFormations() +
                " enCours=" + stats.getFormationsEnCours() +
                " terminées=" + stats.getFormationsTerminees();
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
                "📊 PAR TYPE\n" +
                "• Congés annuels : " + stats.getCongesAnnuels() + "\n" +
                "• Congés maladie : " + stats.getCongesMaladie() + "\n" +
                "• Congés maternité : " + stats.getCongesMaternite() + "\n" +
                "• Absences : " + stats.getAbsences() + "\n\n" +
                "📚 FORMATIONS\n" +
                "• Total : " + stats.getTotalFormations() + "\n" +
                "• En cours : " + stats.getFormationsEnCours() + "\n" +
                "• Terminées : " + stats.getFormationsTerminees() + "\n\n" +
                buildUrgencesRH(stats);
    }

    private String buildUrgencesRH(DashboardStatsDTO stats) {
        StringBuilder sb = new StringBuilder("🚨 URGENCES\n");
        boolean urgent = false;
        if (stats.getAbsencesEnAttente() > 0) {
            sb.append("• ⚠️ ").append(stats.getAbsencesEnAttente())
                    .append(" demande(s) en attente de validation\n");
            urgent = true;
        }
        if (stats.getAnimatricesInactives() > 0) {
            sb.append("• ⚠️ ").append(stats.getAnimatricesInactives())
                    .append(" animatrice(s) inactive(s)\n");
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

    private String buildDernieresAnimatrices(DashboardStatsDTO stats) {
        StringBuilder sb = new StringBuilder("🆕 ANIMATRICES RÉCENTES\n\n");
        if (stats.getDernieresAnimatrices() != null) {
            stats.getDernieresAnimatrices().forEach(a ->
                    sb.append("• ").append(a.getPrenom()).append(" ").append(a.getNom())
                            .append(" — ").append(a.getStatut()).append("\n"));
        }
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
                "✅ Approuvées au total : " + (stats != null ? stats.getAbsencesApprouvees() : "N/A") + "\n" +
                "❌ Refusées au total : " + (stats != null ? stats.getAbsencesRefusees() : "N/A") + "\n" +
                "⏳ En attente : " + (stats != null ? stats.getAbsencesEnAttente() : "N/A") + "\n\n" +
                buildAnalyseParType(stats) + "\n\n" +
                "💡 Recommandations :\n" +
                "• Traiter les demandes en attente sous 24h\n" +
                "• Vérifier l'effectif minimum sur les périodes critiques\n" +
                "• Planifier les congés du mois prochain";
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
                    .append(" | ").append(a.getNbJours()).append(" jours")
                    .append(" | Du ").append(a.getDateDebut().format(fmt))
                    .append(" au ").append(a.getDateFin().format(fmt))
                    .append("\n  → Vérifier quota et effectif avant approbation\n"));
        }
        sb.append("\n💡 ").append(enAttente.size()).append(" demande(s) nécessitent votre action.");
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
        StringBuilder sb = new StringBuilder("✅ ABSENCES APPROUVÉES — " +
                maintenant.getMonth().name() + "\n\n");
        absences.stream()
                .filter(a -> StatutAbsenceConge.APPROUVE.equals(a.getStatut()))
                .filter(a -> a.getDateDebut().getMonthValue() == maintenant.getMonthValue())
                .forEach(a -> sb.append("• ")
                        .append(a.getAnimatrice().getPrenom()).append(" ")
                        .append(a.getAnimatrice().getNom())
                        .append(" | ").append(a.getType().name().replace("_", " "))
                        .append(" | ").append(a.getNbJours()).append(" jours")
                        .append(Boolean.TRUE.equals(a.getDecisionAutomatique()) ? " [AUTO]" : " [MANUEL]")
                        .append("\n"));
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
                    .append(a.getAnimatrice().getPrenom()).append(" ")
                    .append(a.getAnimatrice().getNom())
                    .append(" | ").append(a.getType().name().replace("_", " "))
                    .append(" | ").append(a.getNbJours()).append(" jours")
                    .append(a.getMotifDecision() != null ?
                            "\n  Motif : " + a.getMotifDecision().substring(0, Math.min(80, a.getMotifDecision().length())) : "")
                    .append("\n"));
        }
        return sb.toString();
    }

    private String buildJoursParType(List<AbsenceConge> absences) {
        StringBuilder sb = new StringBuilder();
        for (TypeAbsenceConge type : TypeAbsenceConge.values()) {
            int jours = absences.stream()
                    .filter(a -> type.equals(a.getType()))
                    .filter(a -> StatutAbsenceConge.APPROUVE.equals(a.getStatut()))
                    .mapToInt(a -> a.getNbJours() != null ? a.getNbJours() : 0)
                    .sum();
            sb.append("• ").append(type.name().replace("_", " "))
                    .append(" : ").append(jours).append(" jours\n");
        }
        return sb.toString();
    }

    private String buildAlertesAbsences(List<AbsenceConge> absences,
                                        DashboardStatsDTO stats, List<Animatrice> animatrices) {
        return "🚨 ALERTES ABSENCES\n\n" +
                (stats.getAbsencesEnAttente() > 0 ?
                        "⚠️ " + stats.getAbsencesEnAttente() + " demande(s) en attente de validation\n" : "") +
                buildAlertesQuotas(animatrices) + "\n" +
                "💡 Actions recommandées :\n" +
                "• Traiter les demandes urgentes\n" +
                "• Vérifier les quotas des animatrices proches du maximum\n" +
                "• S'assurer du maintien de l'effectif minimum";
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
        if (!alerteTrouvee) sb.append("✅ Aucun dépassement de quota détecté.\n");
        return sb.toString();
    }

    private String buildAnalyseMoteur(List<AbsenceConge> absences) {
        long autoApprouvees = absences.stream()
                .filter(a -> StatutAbsenceConge.APPROUVE.equals(a.getStatut()))
                .filter(a -> Boolean.TRUE.equals(a.getDecisionAutomatique())).count();
        long autoRefusees = absences.stream()
                .filter(a -> StatutAbsenceConge.REFUSE.equals(a.getStatut()))
                .filter(a -> Boolean.TRUE.equals(a.getDecisionAutomatique())).count();
        long enAttente = absences.stream()
                .filter(a -> StatutAbsenceConge.EN_ATTENTE.equals(a.getStatut())).count();

        return "🤖 ANALYSE MOTEUR DE RÈGLES\n\n" +
                "📊 Décisions automatiques :\n" +
                "• Auto-approuvées : " + autoApprouvees + "\n" +
                "• Auto-refusées : " + autoRefusees + "\n" +
                "• Total automatiques : " + (autoApprouvees + autoRefusees) + "\n" +
                "• Transmises admin : " + enAttente + "\n\n" +
                "📈 Taux d'automatisation : " +
                (absences.size() > 0 ?
                        ((autoApprouvees + autoRefusees) * 100 / absences.size()) + "%" : "N/A") + "\n\n" +
                "💡 Recommandations :\n" +
                "• Activer l'auto-approbation pour réduire la charge admin\n" +
                "• Configurer les quotas selon les besoins réels";
    }

    private String buildContexteFormations(List<Formation> formations, DashboardStatsDTO stats) {
        StringBuilder sb = new StringBuilder("Formations TinySpring :\n");
        formations.forEach(f -> sb.append("• ").append(f.getTitre())
                .append(" | ").append(f.getStatutInscription())
                .append(" | ").append(f.getType()).append("\n"));
        return sb.toString();
    }

    private String buildEtatFormations(List<Formation> formations, DashboardStatsDTO stats) {
        StringBuilder sb = new StringBuilder("📚 ÉTAT DES FORMATIONS\n\n");
        sb.append("• Total : ").append(stats.getTotalFormations()).append("\n");
        sb.append("• Inscrites : ").append(stats.getFormationsInscrites()).append("\n");
        sb.append("• En cours : ").append(stats.getFormationsEnCours()).append("\n");
        sb.append("• Terminées : ").append(stats.getFormationsTerminees()).append("\n\n");
        if (!formations.isEmpty()) {
            sb.append("📋 Liste :\n");
            formations.forEach(f -> sb.append("• ").append(f.getTitre())
                    .append(" — ").append(f.getStatutInscription())
                    .append(" — ").append(f.getType()).append("\n"));
        }
        sb.append("\n💡 Recommandations :\n" +
                "• Encourager les inscriptions aux formations disponibles\n" +
                "• Planifier de nouvelles formations\n" +
                "• Suivre les certifications");
        return sb.toString();
    }

    private String buildFormationsParStatut(List<Formation> formations,
                                            StatutFormation statut, String label) {
        StringBuilder sb = new StringBuilder("📚 FORMATIONS " + label + "\n\n");
        List<Formation> filtrees = formations.stream()
                .filter(f -> statut.equals(f.getStatutInscription()))
                .collect(Collectors.toList());
        if (filtrees.isEmpty()) {
            sb.append("• Aucune formation " + label.toLowerCase() + " actuellement.\n");
        } else {
            filtrees.forEach(f -> sb.append("• ").append(f.getTitre())
                    .append(" — ").append(f.getType())
                    .append(f.getFormateur() != null ? " — " + f.getFormateur() : "")
                    .append("\n"));
        }
        return sb.toString();
    }

    private String buildContexteComplet(DashboardStatsDTO stats) {
        return "TinySpring Garderie — " +
                "Animatrices: " + stats.getTotalAnimatrices() +
                " | Absences: " + stats.getTotalAbsences() +
                " | Approuvées: " + stats.getAbsencesApprouvees() +
                " | Refusées: " + stats.getAbsencesRefusees() +
                " | EnAttente: " + stats.getAbsencesEnAttente() +
                " | Formations: " + stats.getTotalFormations();
    }

    private String buildBilanComplet(DashboardStatsDTO stats) {
        return "📊 BILAN RH COMPLET — TINYSPRING GARDERIE\n\n" +
                "👥 EFFECTIF\n" +
                "• Total animatrices : " + stats.getTotalAnimatrices() + "\n" +
                "• Actives : " + stats.getAnimatricesActives() + "\n" +
                "• Inactives : " + stats.getAnimatricesInactives() + "\n\n" +
                "📋 ABSENCES & CONGÉS\n" +
                "• Total demandes : " + stats.getTotalAbsences() + "\n" +
                "• ✅ Approuvées : " + stats.getAbsencesApprouvees() + "\n" +
                "• ❌ Refusées : " + stats.getAbsencesRefusees() + "\n" +
                "• ⏳ En attente : " + stats.getAbsencesEnAttente() + "\n" +
                "• Taux approbation : " +
                (stats.getTotalAbsences() > 0 ?
                        (stats.getAbsencesApprouvees() * 100 / stats.getTotalAbsences()) + "%" : "N/A") + "\n\n" +
                "📊 PAR TYPE\n" +
                "• Congés annuels : " + stats.getCongesAnnuels() + "\n" +
                "• Congés maladie : " + stats.getCongesMaladie() + "\n" +
                "• Congés maternité : " + stats.getCongesMaternite() + "\n" +
                "• Absences : " + stats.getAbsences() + "\n\n" +
                "📚 FORMATIONS\n" +
                "• Total : " + stats.getTotalFormations() + "\n" +
                "• En cours : " + stats.getFormationsEnCours() + "\n" +
                "• Terminées : " + stats.getFormationsTerminees() + "\n\n" +
                "💡 RECOMMANDATIONS\n" +
                (stats.getAbsencesEnAttente() > 0 ?
                        "• ⚠️ Traiter " + stats.getAbsencesEnAttente() + " demande(s) en attente\n" : "") +
                "• Maintenir l'effectif minimum à 2 animatrices\n" +
                "• Réviser les quotas annuellement\n" +
                "• Planifier les formations du prochain trimestre";
    }

    private Map<String, String> creer(String instruction, String input, String output) {
        Map<String, String> ex = new LinkedHashMap<>();
        ex.put("instruction", instruction);
        ex.put("input", input);
        ex.put("output", output);
        return ex;
    }
}