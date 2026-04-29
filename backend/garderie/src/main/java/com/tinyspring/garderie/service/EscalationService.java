package com.tinyspring.garderie.service;

import com.tinyspring.garderie.dto.EscalationResult;
import com.tinyspring.garderie.entity.Reclamation;

public interface EscalationService {

    /**
     * Analyse une réclamation et retourne la décision d'escalade
     * sans modifier l'entité.
     */
    EscalationResult evaluate(Reclamation reclamation);

    /**
     * Analyse une réclamation et applique l'escalade directement
     * sur l'entité si nécessaire.
     */
    EscalationResult evaluateAndApply(Reclamation reclamation);
}