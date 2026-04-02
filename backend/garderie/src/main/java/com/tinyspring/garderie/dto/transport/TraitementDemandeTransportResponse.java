package com.tinyspring.garderie.dto.transport;

import com.tinyspring.garderie.entity.StatutDemandeTransport;

public record TraitementDemandeTransportResponse(
        Long demandeId,
        StatutDemandeTransport statut,
        Long affectationId,
        Double tauxRemplissageTransport
) {
}
