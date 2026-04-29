package com.tinyspring.garderie.dto.transport.parent;

import java.time.LocalDate;
import java.time.LocalTime;

public record ParentTrajetResponse(
        Long id,
        String pointDepart,
        String destination,
        LocalDate dateTrajet,
        LocalTime heureDepart
) {
}
