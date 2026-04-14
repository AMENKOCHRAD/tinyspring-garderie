package com.tinyspring.garderie.dto.transport;

import com.tinyspring.garderie.entity.transport.SensTrajetDemandeTransport;
import com.tinyspring.garderie.entity.transport.StatutDemandeTransport;

import java.time.LocalDate;
import java.time.LocalTime;

public record DemandeTransportResponse(
        Long id,
        Long enfantId,
        String enfantNomComplet,
        Long parentId,
        String parentNom,
        Long trajetId,
        LocalDate dateDemande,
        String pointDepart,
        String destination,
        LocalDate dateTrajet,
        LocalTime heureDepart,
        StatutDemandeTransport statut,
        String pointRamassage,
        String destinationSouhaitee,
        SensTrajetDemandeTransport sensTrajet,
        String adresseMaison,
        Double latitudeMaison,
        Double longitudeMaison,
        String adresseGarderie
) {
}
