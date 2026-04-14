package com.tinyspring.garderie.dto.transport.admin;

import com.tinyspring.garderie.entity.transport.SensTrajetDemandeTransport;
import com.tinyspring.garderie.entity.transport.StatutDemandeTransport;

import java.time.LocalDate;
import java.time.LocalTime;

public record DemandeAffectationRecommendationResponse(
        Long demandeId,
        Long enfantId,
        String enfantNomComplet,
        StatutDemandeTransport statutDemande,
        SensTrajetDemandeTransport sensTrajet,
        String zoneRecherchee,
        String destinationSouhaitee,
        boolean affectationAutomatiquePossible,
        Integer scorePertinence,
        Double distanceEstimeeKm,
        String modeEvaluation,
        String motifRefus,
        Long trajetRecommandeId,
        String trajetRecommandePointDepart,
        String trajetRecommandeDestination,
        LocalDate trajetRecommandeDate,
        LocalTime trajetRecommandeHeure,
        Long transportRecommandeId,
        String transportRecommandeNom,
        String zoneDesservieTrajet
) {
}
