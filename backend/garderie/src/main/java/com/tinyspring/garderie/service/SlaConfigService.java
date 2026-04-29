package com.tinyspring.garderie.service;

import com.tinyspring.garderie.entity.enums.ReclamationCategory;

/**
 * Service de configuration des délais SLA (Service Level Agreement)
 * par catégorie de réclamation.
 *
 * Le SLA définit le délai maximum (en heures) dans lequel
 * une réclamation doit être traitée selon sa catégorie.
 *
 * Plus le délai SLA est court, plus la réclamation est urgente.
 */
public interface SlaConfigService {

    /**
     * Retourne le délai SLA en heures pour une catégorie donnée.
     *
     * @param category la catégorie de la réclamation
     * @return le délai SLA en heures (ex: 4 = 4 heures max pour traiter)
     */
    int getSlaHours(ReclamationCategory category);

    /**
     * Retourne le pourcentage d'avancement du SLA (0% = tout vient d'être créé,
     * 100% = deadline SLA atteinte, >100% = SLA dépassé).
     *
     * @param category  la catégorie de la réclamation
     * @param ageInMinutes l'âge de la réclamation en minutes depuis sa création
     * @return le pourcentage d'avancement (peut dépasser 100 si SLA dépassé)
     */
    double getSlaProgressPercent(ReclamationCategory category, long ageInMinutes);

    /**
     * Indique si le SLA est dépassé pour une réclamation.
     *
     * @param category     la catégorie de la réclamation
     * @param ageInMinutes l'âge de la réclamation en minutes
     * @return true si le délai SLA a été dépassé
     */
    boolean isSlaBreached(ReclamationCategory category, long ageInMinutes);

    /**
     * Retourne un label lisible de l'urgence SLA pour affichage dans le dashboard.
     *
     * @param category     la catégorie de la réclamation
     * @param ageInMinutes l'âge de la réclamation en minutes
     * @return ex: "SLA DÉPASSÉ", "SLA CRITIQUE (90%)", "SLA OK (40%)"
     */
    String getSlaStatusLabel(ReclamationCategory category, long ageInMinutes);
}
