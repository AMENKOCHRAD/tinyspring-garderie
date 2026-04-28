package com.tinyspring.garderie.service;

import com.tinyspring.garderie.dto.SmartPriorityResult;
import com.tinyspring.garderie.entity.Reclamation;

/**
 * Moteur de calcul de la priorité intelligente (Smart Priority Engine).
 *
 * Calcule un score composite (0 → 100) pour chaque réclamation
 * afin de permettre un tri intelligent dans le dashboard admin.
 *
 * Les réclamations avec le score le plus élevé apparaissent
 * automatiquement en tête de liste pour être traitées en priorité.
 */
public interface SmartPriorityEngine {

    /**
     * Calcule le score de priorité intelligente pour une réclamation.
     *
     * Le score est basé sur 5 composantes :
     *  1. Priorité         (jusqu'à 40 pts) — HIGH=40, MEDIUM=20, LOW=5
     *  2. SLA              (jusqu'à 30 pts) — selon l'avancement du délai SLA
     *  3. Récurrence       (jusqu'à 15 pts) — si la réclamation est récurrente
     *  4. Catégorie        (jusqu'à 10 pts) — bonus catégories critiques (SECURITE)
     *  5. Décision urgente (jusqu'à  5 pts) — bonus si ML recommande action immédiate
     *
     * @param reclamation la réclamation à évaluer
     * @return le résultat complet avec score, niveau, raison détaillée
     */
    SmartPriorityResult calculate(Reclamation reclamation);

    /**
     * Applique directement le résultat du calcul sur l'entité réclamation.
     * Met à jour : smartPriorityScore, smartPriorityLevel, smartPriorityReason.
     *
     * @param reclamation la réclamation à mettre à jour
     * @return le résultat du calcul (pour historique ou log)
     */
    SmartPriorityResult calculateAndApply(Reclamation reclamation);
}
