package com.tinyspring.garderie.service.transport;

import com.tinyspring.garderie.dto.transport.CreateDemandeTransportRequest;
import com.tinyspring.garderie.dto.transport.DemandeTransportResponse;
import com.tinyspring.garderie.dto.transport.TrajetDetailsResponse;
import com.tinyspring.garderie.dto.transport.TraitementDemandeTransportResponse;
import com.tinyspring.garderie.dto.transport.UpdateDemandeTransportRequest;
import com.tinyspring.garderie.dto.transport.admin.AdminDemandPredictionResponse;
import com.tinyspring.garderie.dto.transport.parent.ParentEnfantResponse;
import com.tinyspring.garderie.dto.transport.parent.ParentTrajetResponse;

import java.time.LocalDate;
import java.util.List;

public interface TransportService {
    DemandeTransportResponse creerDemandeTransport(Long parentId, CreateDemandeTransportRequest request);
    DemandeTransportResponse modifierDemandeTransport(Long parentId, Long demandeId, UpdateDemandeTransportRequest request);
    void supprimerDemandeTransport(Long parentId, Long demandeId);
    List<DemandeTransportResponse> listerToutesLesDemandes();
    List<DemandeTransportResponse> listerDemandesParParent(Long parentId);
    List<ParentTrajetResponse> listerTrajetsDisponibles();
    List<ParentEnfantResponse> listerEnfantsParParent(Long parentId);
    TraitementDemandeTransportResponse accepterDemande(Long demandeId);
    TraitementDemandeTransportResponse refuserDemande(Long demandeId);
    void supprimerDemandeTransportAdmin(Long demandeId);
    TrajetDetailsResponse listerEnfantsParTrajet(Long trajetId);
    double calculerTauxRemplissage(Long transportId);
    AdminDemandPredictionResponse predireDemandeAdmin(LocalDate targetDate, Integer hour, boolean rainFlag, boolean schoolBreakFlag);
}
