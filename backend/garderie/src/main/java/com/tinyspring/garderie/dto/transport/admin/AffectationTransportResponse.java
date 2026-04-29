package com.tinyspring.garderie.dto.transport.admin;

import java.time.LocalDate;
import java.time.LocalTime;

public record AffectationTransportResponse(
        Long id,
        Long enfantId,
        String enfantNomComplet,
        Long transportId,
        String transportNom,
        String matriculeTransport,
        Long trajetId,
        String pointDepart,
        String destination,
        LocalDate dateTrajet,
        LocalTime heureDepart,
        String pointRamassage
) {
}
