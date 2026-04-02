package com.tinyspring.garderie.dto.transport.admin;

import java.time.LocalDate;
import java.time.LocalTime;

public record TrajetResponse(
        Long id,
        String pointDepart,
        String destination,
        LocalDate dateTrajet,
        LocalTime heureDepart,
        Long transportId,
        String transportNom,
        String transportMatricule
) {
}
