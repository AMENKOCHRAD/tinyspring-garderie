package com.tinyspring.garderie.dto.transport;

public record EnfantTrajetResponse(
        Long enfantId,
        String nomComplet,
        Long affectationId,
        Long transportId,
        String transportNom,
        String matriculeTransport,
        String pointRamassage
) {
}
