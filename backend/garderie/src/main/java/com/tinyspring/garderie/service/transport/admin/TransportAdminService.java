package com.tinyspring.garderie.service.transport.admin;

import com.tinyspring.garderie.dto.transport.admin.AffectationTransportResponse;
import com.tinyspring.garderie.dto.transport.admin.AdminDemandPredictionResponse;
import com.tinyspring.garderie.dto.transport.admin.DemandeAffectationRecommendationResponse;
import com.tinyspring.garderie.dto.transport.admin.NouveauTrajetRecommendationResponse;
import com.tinyspring.garderie.dto.transport.admin.TrajetRequest;
import com.tinyspring.garderie.dto.transport.admin.TrajetResponse;
import com.tinyspring.garderie.dto.transport.admin.TransportRequest;
import com.tinyspring.garderie.dto.transport.admin.TransportResponse;

import java.time.LocalDate;
import java.util.List;

public interface TransportAdminService {
    List<TransportResponse> listerTransports();
    TransportResponse creerTransport(TransportRequest request);
    TransportResponse modifierTransport(Long id, TransportRequest request);
    void supprimerTransport(Long id);
    List<TrajetResponse> listerTrajets();
    TrajetResponse creerTrajet(TrajetRequest request);
    TrajetResponse modifierTrajet(Long id, TrajetRequest request);
    void supprimerTrajet(Long id);
    List<AffectationTransportResponse> listerAffectations();
    List<DemandeAffectationRecommendationResponse> listerRecommandationsAffectation();
    List<NouveauTrajetRecommendationResponse> listerRecommandationsNouveauxTrajets();
    AdminDemandPredictionResponse predireDemandeAdmin(LocalDate targetDate, Integer hour, boolean rainFlag, boolean schoolBreakFlag);
}
