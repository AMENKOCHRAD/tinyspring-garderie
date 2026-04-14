package com.tinyspring.garderie.dto.transport.admin;

import java.util.List;

public record NouveauTrajetRecommendationResponse(
        String zoneCentrale,
        Double latitudeCentre,
        Double longitudeCentre,
        int nombreDemandes,
        Double distanceMoyenneAuTrajetLePlusProcheKm,
        String recommandation,
        List<NouveauTrajetSuggestionDemandeResponse> demandes
) {
}
