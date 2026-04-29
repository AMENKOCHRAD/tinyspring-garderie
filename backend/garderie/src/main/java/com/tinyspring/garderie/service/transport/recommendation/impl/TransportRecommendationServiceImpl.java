package com.tinyspring.garderie.service.transport.recommendation.impl;

import com.tinyspring.garderie.dto.transport.admin.DemandeAffectationRecommendationResponse;
import com.tinyspring.garderie.dto.transport.admin.NouveauTrajetRecommendationResponse;
import com.tinyspring.garderie.dto.transport.admin.NouveauTrajetSuggestionDemandeResponse;
import com.tinyspring.garderie.entity.transport.AffectationTransport;
import com.tinyspring.garderie.entity.transport.DemandeTransport;
import com.tinyspring.garderie.entity.transport.SensTrajetDemandeTransport;
import com.tinyspring.garderie.entity.transport.StatutDemandeTransport;
import com.tinyspring.garderie.entity.transport.Trajet;
import com.tinyspring.garderie.exception.BusinessException;
import com.tinyspring.garderie.repository.transport.AffectationTransportRepository;
import com.tinyspring.garderie.repository.transport.DemandeTransportRepository;
import com.tinyspring.garderie.repository.transport.TrajetRepository;
import com.tinyspring.garderie.service.transport.recommendation.TransportRecommendationEngine;
import com.tinyspring.garderie.service.transport.recommendation.TransportRecommendationService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class TransportRecommendationServiceImpl implements TransportRecommendationService {

    private final DemandeTransportRepository demandeTransportRepository;
    private final AffectationTransportRepository affectationTransportRepository;
    private final TrajetRepository trajetRepository;
    private final TransportRecommendationEngine recommendationEngine;

    public TransportRecommendationServiceImpl(DemandeTransportRepository demandeTransportRepository,
                                              AffectationTransportRepository affectationTransportRepository,
                                              TrajetRepository trajetRepository,
                                              TransportRecommendationEngine recommendationEngine) {
        this.demandeTransportRepository = demandeTransportRepository;
        this.affectationTransportRepository = affectationTransportRepository;
        this.trajetRepository = trajetRepository;
        this.recommendationEngine = recommendationEngine;
    }

    @Override
    public List<DemandeAffectationRecommendationResponse> getRecommendationsAffectation() {
        List<DemandeTransport> allDemandes = demandeTransportRepository.findAllByOrderByIdDesc();
        List<AffectationTransport> affectations = affectationTransportRepository.findAll();
        List<Trajet> candidateTrajets = findCandidateTrajets(affectations);

        return findDemandesAAssister(allDemandes, affectations).stream()
                .map(demande -> recommendationEngine.recommendDemande(
                        demande,
                        filterCandidateTrajetsForDemande(candidateTrajets, demande),
                        allDemandes,
                        affectations
                ))
                .map(this::toDemandeRecommendationResponse)
                .toList();
    }

    @Override
    public List<NouveauTrajetRecommendationResponse> getRecommendationsNouveauxTrajets() {
        List<DemandeTransport> allDemandes = demandeTransportRepository.findAllByOrderByIdDesc();
        List<AffectationTransport> affectations = affectationTransportRepository.findAll();
        List<Trajet> candidateTrajets = findCandidateTrajets(affectations);

        List<TransportRecommendationEngine.DemandeRecommendationDecision> decisions = findDemandesAAssister(allDemandes, affectations).stream()
                .map(demande -> recommendationEngine.recommendDemande(
                        demande,
                        filterCandidateTrajetsForDemande(candidateTrajets, demande),
                        allDemandes,
                        affectations
                ))
                .toList();

        return recommendationEngine.buildNewRouteSuggestions(decisions).stream()
                .map(this::toNouveauTrajetRecommendationResponse)
                .toList();
    }

    @Override
    public TransportRecommendationEngine.DemandeRecommendationDecision recommanderPourDemande(DemandeTransport demande) {
        List<DemandeTransport> allDemandes = demandeTransportRepository.findAllByOrderByIdDesc();
        List<AffectationTransport> affectations = affectationTransportRepository.findAll();
        List<Trajet> candidateTrajets = findCandidateTrajets(affectations);
        return recommendationEngine.recommendDemande(
                demande,
                filterCandidateTrajetsForDemande(candidateTrajets, demande),
                allDemandes,
                affectations
        );
    }

    @Override
    public Trajet validerEtRetournerTrajetPourAffectation(DemandeTransport demande) {
        TransportRecommendationEngine.DemandeRecommendationDecision decision = recommanderPourDemande(demande);
        if (!decision.affectationAutomatiquePossible() || decision.recommendationCandidate() == null) {
            throw new BusinessException(decision.motifRefus() != null
                    ? decision.motifRefus()
                    : "Aucun trajet adequat n'a ete trouve pour cette demande");
        }
        return decision.recommendationCandidate().trajet();
    }

    private List<DemandeTransport> findDemandesAAssister(List<DemandeTransport> demandes, List<AffectationTransport> affectations) {
        return demandes.stream()
                .filter(demande -> demande.getStatut() != StatutDemandeTransport.REFUSEE)
                .filter(demande -> demande.getTrajet() == null)
                .filter(demande -> affectations.stream().noneMatch(affectation -> affectation.getEnfant().getId().equals(demande.getEnfant().getId())))
                .toList();
    }

    private List<Trajet> findCandidateTrajets(List<AffectationTransport> affectations) {
        return trajetRepository.findByDateTrajetGreaterThanEqualOrderByDateTrajetAscHeureDepartAsc(LocalDate.now()).stream()
                .filter(trajet -> countAffectationsForTrajet(affectations, trajet.getId()) < trajet.getTransport().getCapacite())
                .toList();
    }

    private long countAffectationsForTrajet(List<AffectationTransport> affectations, Long trajetId) {
        return affectations.stream()
                .filter(affectation -> affectation.getTrajet().getId().equals(trajetId))
                .count();
    }

    private List<Trajet> filterCandidateTrajetsForDemande(List<Trajet> candidateTrajets, DemandeTransport demande) {
        LocalDate requestedDate = demande.getDateSouhaitee();

        return candidateTrajets.stream()
                .filter(trajet -> requestedDate == null || requestedDate.equals(trajet.getDateTrajet()))
                .filter(trajet -> inferTrajetSense(trajet) == demande.getSensTrajet())
                .sorted(Comparator.comparing(Trajet::getDateTrajet).thenComparing(Trajet::getHeureDepart))
                .toList();
    }

    private SensTrajetDemandeTransport inferTrajetSense(Trajet trajet) {
        return isGarderieAddress(trajet.getPointDepart())
                ? SensTrajetDemandeTransport.GARDERIE_VERS_MAISON
                : SensTrajetDemandeTransport.MAISON_VERS_GARDERIE;
    }

    private boolean isGarderieAddress(String address) {
        return address != null
                && address.trim().equalsIgnoreCase("15 Rue des Ecoles, El Menzah 5, Ariana 2091, Tunisie");
    }

    private DemandeAffectationRecommendationResponse toDemandeRecommendationResponse(TransportRecommendationEngine.DemandeRecommendationDecision decision) {
        DemandeTransport demande = decision.demande();
        TransportRecommendationEngine.RecommendationCandidate candidate = decision.recommendationCandidate();
        Trajet trajet = candidate != null ? candidate.trajet() : null;

        return new DemandeAffectationRecommendationResponse(
                demande.getId(),
                demande.getEnfant().getId(),
                demande.getEnfant().getPrenom() + " " + demande.getEnfant().getNom(),
                demande.getStatut(),
                demande.getSensTrajet(),
                decision.zoneRecherchee().zoneLabel(),
                demande.getDestinationSouhaitee(),
                decision.affectationAutomatiquePossible(),
                decision.scorePertinence(),
                decision.distanceEstimeeKm(),
                decision.modeEvaluation(),
                decision.motifRefus(),
                trajet != null ? trajet.getId() : null,
                trajet != null ? trajet.getPointDepart() : null,
                trajet != null ? trajet.getDestination() : null,
                trajet != null ? trajet.getDateTrajet() : null,
                trajet != null ? trajet.getHeureDepart() : null,
                trajet != null ? trajet.getTransport().getId() : null,
                trajet != null ? trajet.getTransport().getNom() : null,
                trajet != null ? trajet.getZoneDesservie() : null
        );
    }

    private NouveauTrajetRecommendationResponse toNouveauTrajetRecommendationResponse(TransportRecommendationEngine.NouveauTrajetSuggestion suggestion) {
        List<NouveauTrajetSuggestionDemandeResponse> demandes = suggestion.demandes().stream()
                .map(TransportRecommendationEngine.DemandeRecommendationDecision::demande)
                .filter(Objects::nonNull)
                .map(demande -> new NouveauTrajetSuggestionDemandeResponse(
                        demande.getId(),
                        demande.getEnfant().getId(),
                        demande.getEnfant().getPrenom() + " " + demande.getEnfant().getNom(),
                        demande.getSensTrajet(),
                        demande.getSensTrajet() == SensTrajetDemandeTransport.GARDERIE_VERS_MAISON
                                ? demande.getDestinationSouhaitee()
                                : demande.getPointRamassage(),
                        demande.getDestinationSouhaitee()
                ))
                .toList();

        return new NouveauTrajetRecommendationResponse(
                suggestion.zoneCentrale(),
                suggestion.latitudeCentre(),
                suggestion.longitudeCentre(),
                demandes.size(),
                suggestion.distanceMoyenneAuTrajetLePlusProcheKm(),
                suggestion.recommandation(),
                demandes
        );
    }
}
