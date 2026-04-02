package com.tinyspring.garderie.dto.transport;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record TrajetDetailsResponse(
        Long trajetId,
        String pointDepart,
        String destination,
        LocalDate dateTrajet,
        LocalTime heureDepart,
        long totalEnfantsAffectes,
        List<EnfantTrajetResponse> enfants
) {
}
