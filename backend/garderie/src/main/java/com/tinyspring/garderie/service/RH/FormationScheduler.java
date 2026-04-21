package com.tinyspring.garderie.service.RH;

import com.tinyspring.garderie.entity.RH.Formation;
import com.tinyspring.garderie.entity.RH.enums.StatutFormation;
import com.tinyspring.garderie.repository.RH.FormationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FormationScheduler {

    private final FormationRepository formationRepository;
    private final FormationService formationService;

    @Scheduled(fixedRate = 60000)
    public void gererCycleVieAutomatique() {
        LocalDate aujourd_hui = LocalDate.now();
        LocalTime maintenant = LocalTime.now();

        System.out.println("🕐 [Scheduler] Vérification à " + maintenant.toString().substring(0, 5)
                + " — date : " + aujourd_hui);

        // ===== AUTO-DÉMARRAGE =====
        List<Formation> aDemar = formationRepository
                .findByStatut(StatutFormation.OUVERTE).stream()
                .filter(f ->
                        f.getDateFormation() != null &&
                                f.getHeureDebut() != null &&
                                f.getDateFormation().equals(aujourd_hui) &&
                                !maintenant.isBefore(f.getHeureDebut())
                )
                .toList();

        for (Formation f : aDemar) {
            try {
                // ✅ Utilise demarrerAutomatique — sans vérification d'inscrits
                formationService.demarrerAutomatique(f.getId());
                System.out.println("🚀 [AUTO] Formation démarrée : " + f.getTitre()
                        + " (prévu à " + f.getHeureDebut() + ")");
            } catch (Exception e) {
                System.err.println("⚠️ Erreur auto-démarrage " + f.getId() + " : " + e.getMessage());
            }
        }

        // ===== AUTO-TERMINAISON =====
        List<Formation> aTerminer = formationRepository
                .findByStatut(StatutFormation.EN_COURS).stream()
                .filter(f ->
                        f.getDateFormation() != null &&
                                f.getHeureFin() != null &&
                                f.getDateFormation().equals(aujourd_hui) &&
                                !maintenant.isBefore(f.getHeureFin())
                )
                .toList();

        for (Formation f : aTerminer) {
            try {
                formationService.terminerFormation(f.getId());
                System.out.println("✅ [AUTO] Formation terminée : " + f.getTitre()
                        + " (prévu à " + f.getHeureFin() + ")");
            } catch (Exception e) {
                System.err.println("⚠️ Erreur auto-terminaison " + f.getId() + " : " + e.getMessage());
            }
        }

        // ===== FORMATIONS PASSÉES NON TERMINÉES =====
        List<Formation> passees = formationRepository
                .findByStatut(StatutFormation.EN_COURS).stream()
                .filter(f ->
                        f.getDateFormation() != null &&
                                f.getDateFormation().isBefore(aujourd_hui)
                )
                .toList();

        for (Formation f : passees) {
            try {
                formationService.terminerFormation(f.getId());
                System.out.println("✅ [AUTO] Formation passée terminée : " + f.getTitre());
            } catch (Exception e) {
                System.err.println("⚠️ " + f.getId() + " : " + e.getMessage());
            }
        }
    }
}