package com.tinyspring.garderie.dto.transport;

import com.tinyspring.garderie.entity.StatutDemandeTransport;

import java.time.LocalDate;
import java.time.LocalTime;

public record DemandeTransportResponse(
        Long id,
        Long enfantId,
        String enfantNomComplet,
        Long parentId,
        String parentNom,
        Long trajetId,
        String pointDepart,
        String destination,
        LocalDate dateTrajet,
        LocalTime heureDepart,
        StatutDemandeTransport statut,
        String pointRamassage
) {
}
