package com.tinyspring.garderie.service.transport.admin.impl;

import com.tinyspring.garderie.dto.transport.admin.AffectationTransportResponse;
import com.tinyspring.garderie.dto.transport.admin.AdminDemandPredictionResponse;
import com.tinyspring.garderie.dto.transport.admin.DemandeAffectationRecommendationResponse;
import com.tinyspring.garderie.dto.transport.admin.NouveauTrajetRecommendationResponse;
import com.tinyspring.garderie.dto.transport.admin.TrajetRequest;
import com.tinyspring.garderie.dto.transport.admin.TrajetResponse;
import com.tinyspring.garderie.dto.transport.admin.TransportRequest;
import com.tinyspring.garderie.dto.transport.admin.TransportResponse;
import com.tinyspring.garderie.entity.transport.AffectationTransport;
import com.tinyspring.garderie.entity.transport.Enfant;
import com.tinyspring.garderie.entity.transport.Trajet;
import com.tinyspring.garderie.entity.transport.Transport;
import com.tinyspring.garderie.exception.BusinessException;
import com.tinyspring.garderie.exception.ResourceNotFoundException;
import com.tinyspring.garderie.repository.transport.AffectationTransportRepository;
import com.tinyspring.garderie.repository.transport.DemandeTransportRepository;
import com.tinyspring.garderie.repository.transport.TrajetRepository;
import com.tinyspring.garderie.repository.transport.TransportRepository;
import com.tinyspring.garderie.service.transport.TransportService;
import com.tinyspring.garderie.service.transport.admin.TransportAdminService;
import com.tinyspring.garderie.service.transport.recommendation.TransportRecommendationService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class TransportAdminServiceImpl implements TransportAdminService {

    private final TransportRepository transportRepository;
    private final TrajetRepository trajetRepository;
    private final AffectationTransportRepository affectationTransportRepository;
    private final DemandeTransportRepository demandeTransportRepository;
    private final TransportService transportService;
    private final TransportRecommendationService transportRecommendationService;

    public TransportAdminServiceImpl(TransportRepository transportRepository,
                                     TrajetRepository trajetRepository,
                                     AffectationTransportRepository affectationTransportRepository,
                                     DemandeTransportRepository demandeTransportRepository,
                                     TransportService transportService,
                                     TransportRecommendationService transportRecommendationService) {
        this.transportRepository = transportRepository;
        this.trajetRepository = trajetRepository;
        this.affectationTransportRepository = affectationTransportRepository;
        this.demandeTransportRepository = demandeTransportRepository;
        this.transportService = transportService;
        this.transportRecommendationService = transportRecommendationService;
    }

    @Override
    public List<TransportResponse> listerTransports() {
        return transportRepository.findAll()
                .stream()
                .map(this::toTransportResponse)
                .toList();
    }

    @Override
    public TransportResponse creerTransport(TransportRequest request) {
        Transport transport = new Transport(request.getNom(), request.getMatricule(), request.getCapacite());
        return toTransportResponse(transportRepository.save(transport));
    }

    @Override
    public TransportResponse modifierTransport(Long id, TransportRequest request) {
        Transport transport = getTransport(id);
        transport.setNom(request.getNom());
        transport.setMatricule(request.getMatricule());
        transport.setCapacite(request.getCapacite());
        return toTransportResponse(transportRepository.save(transport));
    }

    @Override
    public void supprimerTransport(Long id) {
        if (affectationTransportRepository.countByTransportId(id) > 0) {
            throw new BusinessException("Impossible de supprimer un transport deja utilise dans une affectation");
        }

        boolean trajetAssocie = trajetRepository.findAll().stream().anyMatch(trajet -> trajet.getTransport().getId().equals(id));
        if (trajetAssocie) {
            throw new BusinessException("Impossible de supprimer un transport deja associe a un trajet");
        }

        transportRepository.delete(getTransport(id));
    }

    @Override
    public List<TrajetResponse> listerTrajets() {
        return trajetRepository.findAll()
                .stream()
                .map(this::toTrajetResponse)
                .toList();
    }

    @Override
    public TrajetResponse creerTrajet(TrajetRequest request) {
        Transport transport = getTransport(request.getTransportId());
        Trajet trajet = new Trajet(
                request.getPointDepart(),
                request.getDestination(),
                request.getDateTrajet(),
                request.getHeureDepart(),
                transport
        );
        trajet.setZoneDesservie(request.getZoneDesservie());
        trajet.setLatitudeDestination(request.getLatitudeDestination());
        trajet.setLongitudeDestination(request.getLongitudeDestination());
        return toTrajetResponse(trajetRepository.save(trajet));
    }

    @Override
    public TrajetResponse modifierTrajet(Long id, TrajetRequest request) {
        Trajet trajet = getTrajet(id);
        trajet.setPointDepart(request.getPointDepart());
        trajet.setDestination(request.getDestination());
        trajet.setDateTrajet(request.getDateTrajet());
        trajet.setHeureDepart(request.getHeureDepart());
        trajet.setTransport(getTransport(request.getTransportId()));
        trajet.setZoneDesservie(request.getZoneDesservie());
        trajet.setLatitudeDestination(request.getLatitudeDestination());
        trajet.setLongitudeDestination(request.getLongitudeDestination());
        return toTrajetResponse(trajetRepository.save(trajet));
    }

    @Override
    public void supprimerTrajet(Long id) {
        if (!affectationTransportRepository.findByTrajetId(id).isEmpty()) {
            throw new BusinessException("Impossible de supprimer un trajet deja utilise dans une affectation");
        }
        if (demandeTransportRepository.existsByTrajetId(id)) {
            throw new BusinessException("Impossible de supprimer un trajet deja utilise dans une demande");
        }
        trajetRepository.delete(getTrajet(id));
    }

    @Override
    public List<AffectationTransportResponse> listerAffectations() {
        return affectationTransportRepository.findAll()
                .stream()
                .map(this::toAffectationResponse)
                .toList();
    }

    @Override
    public List<DemandeAffectationRecommendationResponse> listerRecommandationsAffectation() {
        return transportRecommendationService.getRecommendationsAffectation();
    }

    @Override
    public List<NouveauTrajetRecommendationResponse> listerRecommandationsNouveauxTrajets() {
        return transportRecommendationService.getRecommendationsNouveauxTrajets();
    }

    @Override
    public AdminDemandPredictionResponse predireDemandeAdmin(LocalDate targetDate, Integer hour, boolean rainFlag, boolean schoolBreakFlag) {
        return transportService.predireDemandeAdmin(targetDate, hour, rainFlag, schoolBreakFlag);
    }

    private Transport getTransport(Long id) {
        return transportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transport introuvable avec l'id " + id));
    }

    private Trajet getTrajet(Long id) {
        return trajetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trajet introuvable avec l'id " + id));
    }

    private TransportResponse toTransportResponse(Transport transport) {
        double taux = transportService.calculerTauxRemplissage(transport.getId());
        return new TransportResponse(
                transport.getId(),
                transport.getNom(),
                transport.getMatricule(),
                transport.getCapacite(),
                taux
        );
    }

    private TrajetResponse toTrajetResponse(Trajet trajet) {
        return new TrajetResponse(
                trajet.getId(),
                trajet.getPointDepart(),
                trajet.getDestination(),
                trajet.getZoneDesservie(),
                trajet.getLatitudeDestination(),
                trajet.getLongitudeDestination(),
                trajet.getDateTrajet(),
                trajet.getHeureDepart(),
                trajet.getTransport().getId(),
                trajet.getTransport().getNom(),
                trajet.getTransport().getMatricule()
        );
    }

    private AffectationTransportResponse toAffectationResponse(AffectationTransport affectation) {
        Enfant enfant = affectation.getEnfant();
        return new AffectationTransportResponse(
                affectation.getId(),
                enfant.getId(),
                enfant.getPrenom() + " " + enfant.getNom(),
                affectation.getTransport().getId(),
                affectation.getTransport().getNom(),
                affectation.getTransport().getMatricule(),
                affectation.getTrajet().getId(),
                affectation.getTrajet().getPointDepart(),
                affectation.getTrajet().getDestination(),
                affectation.getTrajet().getDateTrajet(),
                affectation.getTrajet().getHeureDepart(),
                affectation.getPointRamassage()
        );
    }
}
