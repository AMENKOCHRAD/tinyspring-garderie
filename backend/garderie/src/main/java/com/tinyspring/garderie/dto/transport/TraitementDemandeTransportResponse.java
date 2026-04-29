package com.tinyspring.garderie.dto.transport;

import com.tinyspring.garderie.entity.transport.StatutDemandeTransport;

public record TraitementDemandeTransportResponse(
        Long demandeId,
        StatutDemandeTransport statut,
        Long affectationId,
        Double tauxRemplissageTransport,
        String message
) {
}
