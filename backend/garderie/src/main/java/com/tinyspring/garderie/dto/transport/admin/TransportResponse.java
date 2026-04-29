package com.tinyspring.garderie.dto.transport.admin;

public record TransportResponse(
        Long id,
        String nom,
        String matricule,
        Integer capacite,
        Double tauxRemplissage
) {
}
