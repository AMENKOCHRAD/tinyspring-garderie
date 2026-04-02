package com.tinyspring.garderie.service;

import com.tinyspring.garderie.dto.transport.CreateDemandeTransportRequest;
import com.tinyspring.garderie.dto.transport.DemandeTransportResponse;
import com.tinyspring.garderie.dto.transport.TrajetDetailsResponse;
import com.tinyspring.garderie.dto.transport.TraitementDemandeTransportResponse;

import java.util.List;

public interface TransportService {
    DemandeTransportResponse creerDemandeTransport(Long parentId, CreateDemandeTransportRequest request);
    List<DemandeTransportResponse> listerToutesLesDemandes();
    List<DemandeTransportResponse> listerDemandesParParent(Long parentId);
    TraitementDemandeTransportResponse accepterDemande(Long demandeId, Long transportId);
    TraitementDemandeTransportResponse refuserDemande(Long demandeId);
    TrajetDetailsResponse listerEnfantsParTrajet(Long trajetId);
    double calculerTauxRemplissage(Long transportId);
}
