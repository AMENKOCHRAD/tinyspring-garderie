package com.tinyspring.garderie.service.impl;

import com.tinyspring.garderie.entity.enums.ReclamationCategory;
import com.tinyspring.garderie.service.SlaConfigService;
import org.springframework.stereotype.Service;

/**
 * Implémentation des règles SLA (Service Level Agreement) par catégorie.
 *
 * Logique métier :
 * ─────────────────────────────────────────────────────────────────
 *  Catégorie        │ SLA (heures) │ Justification
 * ──────────────────┼──────────────┼────────────────────────────────
 *  SECURITE         │      4h      │ Urgence absolue (sécurité enfants)
 *  TRANSPORT        │      8h      │ Impact opérationnel immédiat
 *  REPAS            │     12h      │ Risque santé (allergie, qualité)
 *  COMPORTEMENT     │     24h      │ Risque psychologique / social
 *  HYGIENE          │     24h      │ Risque sanitaire
 *  PERSONNEL        │     48h      │ Gestion RH, suivi direction
 *  PEDAGOGIQUE      │     48h      │ Suivi pédagogique 
 *  FINANCIER        │     72h      │ Traitement administratif/comptable
 *  ADMINISTRATIF    │     72h      │ Traitement de dossier
 *  AUTRE            │     96h      │ Réclamation générale non classifiée
 * ─────────────────────────────────────────────────────────────────
 */
@Service
public class SlaConfigServiceImpl implements SlaConfigService {

    /**
     * Seuils d'alerte SLA (en % de progression) :
     *  - Si avancement >= 100% → SLA DÉPASSÉ (rouge vif)
     *  - Si avancement >= 75%  → SLA CRITIQUE (orange)
     *  - Si avancement >= 50%  → SLA EN DANGER (jaune)
     *  - Sinon                 → SLA OK (vert)
     */
    private static final double BREACH_THRESHOLD   = 100.0;
    private static final double CRITICAL_THRESHOLD =  75.0;
    private static final double WARNING_THRESHOLD  =  50.0;

    @Override
    public int getSlaHours(ReclamationCategory category) {
        if (category == null) {
            return 96; // Défaut sécurisé pour catégorie inconnue
        }

        return switch (category) {
            case SECURITE      ->  4;
            case TRANSPORT     ->  8;
            case REPAS         -> 12;
            case COMPORTEMENT  -> 24;
            case HYGIENE       -> 24;
            case PERSONNEL     -> 48;
            case PEDAGOGIQUE   -> 48;
            case FINANCIER     -> 72;
            case ADMINISTRATIF -> 72;
            case AUTRE         -> 96;
        };
    }

    @Override
    public double getSlaProgressPercent(ReclamationCategory category, long ageInMinutes) {
        int slaMinutes = getSlaHours(category) * 60;

        if (slaMinutes <= 0) {
            return 100.0;
        }

        double progress = ((double) ageInMinutes / slaMinutes) * 100.0;

        // Arrondi à 2 décimales pour la lisibilité
        return Math.round(progress * 100.0) / 100.0;
    }

    @Override
    public boolean isSlaBreached(ReclamationCategory category, long ageInMinutes) {
        return getSlaProgressPercent(category, ageInMinutes) >= BREACH_THRESHOLD;
    }

    @Override
    public String getSlaStatusLabel(ReclamationCategory category, long ageInMinutes) {
        double progress = getSlaProgressPercent(category, ageInMinutes);

        if (progress >= BREACH_THRESHOLD) {
            long overdueMinutes = ageInMinutes - (long) getSlaHours(category) * 60;
            String overdueText = formatDuration(overdueMinutes);
            return "SLA DÉPASSÉ de " + overdueText;
        }

        if (progress >= CRITICAL_THRESHOLD) {
            int remainingMinutes = (int) ((getSlaHours(category) * 60) - ageInMinutes);
            return "SLA CRITIQUE — " + formatDuration(remainingMinutes) + " restant(s)";
        }

        if (progress >= WARNING_THRESHOLD) {
            int remainingMinutes = (int) ((getSlaHours(category) * 60) - ageInMinutes);
            return "SLA EN DANGER — " + formatDuration(remainingMinutes) + " restant(s)";
        }

        int remainingMinutes = (int) ((getSlaHours(category) * 60) - ageInMinutes);
        return "SLA OK — " + formatDuration(remainingMinutes) + " restant(s)";
    }

    /**
     * Formate une durée en minutes en texte lisible.
     * Ex: 130 minutes → "2h 10min"
     *     45 minutes  → "45min"
     *
     * @param totalMinutes durée en minutes
     * @return texte formaté
     */
    private String formatDuration(long totalMinutes) {
        if (totalMinutes <= 0) {
            return "0min";
        }

        long hours   = totalMinutes / 60;
        long minutes = totalMinutes % 60;

        if (hours == 0) {
            return minutes + "min";
        }

        if (minutes == 0) {
            return hours + "h";
        }

        return hours + "h " + minutes + "min";
    }
}
