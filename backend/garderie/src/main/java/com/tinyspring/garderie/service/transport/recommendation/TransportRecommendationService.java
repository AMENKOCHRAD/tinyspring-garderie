package com.tinyspring.garderie.service.transport.recommendation;

import com.tinyspring.garderie.dto.transport.admin.DemandeAffectationRecommendationResponse;
import com.tinyspring.garderie.dto.transport.admin.NouveauTrajetRecommendationResponse;
import com.tinyspring.garderie.entity.transport.DemandeTransport;
import com.tinyspring.garderie.entity.transport.Trajet;

import java.util.List;

public interface TransportRecommendationService {
    List<DemandeAffectationRecommendationResponse> getRecommendationsAffectation();
    List<NouveauTrajetRecommendationResponse> getRecommendationsNouveauxTrajets();
    TransportRecommendationEngine.DemandeRecommendationDecision recommanderPourDemande(DemandeTransport demande);
    Trajet validerEtRetournerTrajetPourAffectation(DemandeTransport demande);
}
