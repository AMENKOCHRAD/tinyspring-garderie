package com.tinyspring.garderie.dto.transport.admin;

import com.tinyspring.garderie.entity.transport.SensTrajetDemandeTransport;

public record NouveauTrajetSuggestionDemandeResponse(
        Long demandeId,
        Long enfantId,
        String enfantNomComplet,
        SensTrajetDemandeTransport sensTrajet,
        String zoneRecherchee,
        String destinationSouhaitee
) {
}
