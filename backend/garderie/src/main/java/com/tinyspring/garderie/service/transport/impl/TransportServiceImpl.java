package com.tinyspring.garderie.service.transport.impl;

import com.tinyspring.garderie.dto.transport.CreateDemandeTransportRequest;
import com.tinyspring.garderie.dto.transport.DemandeTransportResponse;
import com.tinyspring.garderie.dto.transport.EnfantTrajetResponse;
import com.tinyspring.garderie.dto.transport.TrajetDetailsResponse;
import com.tinyspring.garderie.dto.transport.TraitementDemandeTransportResponse;
import com.tinyspring.garderie.entity.RoleName;
import com.tinyspring.garderie.entity.User;
import com.tinyspring.garderie.entity.transport.AffectationTransport;
import com.tinyspring.garderie.entity.transport.DemandeTransport;
import com.tinyspring.garderie.entity.transport.Enfant;
import com.tinyspring.garderie.entity.transport.StatutDemandeTransport;
import com.tinyspring.garderie.entity.transport.Trajet;
import com.tinyspring.garderie.entity.transport.Transport;
import com.tinyspring.garderie.exception.BusinessException;
import com.tinyspring.garderie.exception.ResourceNotFoundException;
import com.tinyspring.garderie.repository.UserRepository;
import com.tinyspring.garderie.repository.transport.AffectationTransportRepository;
import com.tinyspring.garderie.repository.transport.DemandeTransportRepository;
import com.tinyspring.garderie.repository.transport.EnfantRepository;
import com.tinyspring.garderie.repository.transport.TrajetRepository;
import com.tinyspring.garderie.repository.transport.TransportRepository;
import com.tinyspring.garderie.service.transport.TransportService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class TransportServiceImpl implements TransportService {

    private static final Logger logger = LoggerFactory.getLogger(TransportServiceImpl.class);

    private final DemandeTransportRepository demandeTransportRepository;
    private final AffectationTransportRepository affectationTransportRepository;
    private final EnfantRepository enfantRepository;
    private final TransportRepository transportRepository;
    private final TrajetRepository trajetRepository;
    private final UserRepository userRepository;

    public TransportServiceImpl(DemandeTransportRepository demandeTransportRepository,
                                AffectationTransportRepository affectationTransportRepository,
                                EnfantRepository enfantRepository,
                                TransportRepository transportRepository,
                                TrajetRepository trajetRepository,
                                UserRepository userRepository) {
        this.demandeTransportRepository = demandeTransportRepository;
        this.affectationTransportRepository = affectationTransportRepository;
        this.enfantRepository = enfantRepository;
        this.transportRepository = transportRepository;
        this.trajetRepository = trajetRepository;
        this.userRepository = userRepository;
    }

    @Override
    public DemandeTransportResponse creerDemandeTransport(Long parentId, CreateDemandeTransportRequest request) {
        User parent = getUser(parentId);
        if (parent.getRole().getName() != RoleName.PARENT) {
            throw new BusinessException("Seul un parent peut effectuer une demande de transport");
        }

        Enfant enfant = enfantRepository.findByIdAndParentId(request.getEnfantId(), parentId)
                .orElseThrow(() -> new BusinessException("Cet enfant n'appartient pas au parent connecte"));
        Trajet trajet = getTrajet(request.getTrajetId());
        if (!trajet.getDateTrajet().isAfter(LocalDate.now())) {
            throw new BusinessException("Le parent ne peut faire une demande que pour un trajet a partir de demain");
        }

        boolean demandeActive = demandeTransportRepository.existsByEnfantIdAndStatut(enfant.getId(), StatutDemandeTransport.EN_ATTENTE)
                || demandeTransportRepository.existsByEnfantIdAndStatut(enfant.getId(), StatutDemandeTransport.ACCEPTEE);
        if (demandeActive) {
            throw new BusinessException("Une demande active existe deja pour cet enfant");
        }

        DemandeTransport demande = new DemandeTransport(
                enfant,
                parent,
                trajet,
                StatutDemandeTransport.EN_ATTENTE,
                request.getPointRamassage()
        );

        DemandeTransport savedDemande = demandeTransportRepository.save(demande);
        logger.info("Demande de transport creee. demandeId={}, enfantId={}, parentId={}",
                savedDemande.getId(), enfant.getId(), parentId);

        return toDemandeResponse(savedDemande);
    }

    @Override
    @Transactional
    public List<DemandeTransportResponse> listerToutesLesDemandes() {
        return demandeTransportRepository.findAllByOrderByIdDesc()
                .stream()
                .map(this::toDemandeResponse)
                .toList();
    }

    @Override
    @Transactional
    public List<DemandeTransportResponse> listerDemandesParParent(Long parentId) {
        return demandeTransportRepository.findByParentIdOrderByIdDesc(parentId)
                .stream()
                .map(this::toDemandeResponse)
                .toList();
    }

    @Override
    public TraitementDemandeTransportResponse accepterDemande(Long demandeId, Long transportId) {
        DemandeTransport demande = getDemande(demandeId);
        Transport transport = getTransport(transportId);

        if (demande.getStatut() != StatutDemandeTransport.EN_ATTENTE) {
            throw new BusinessException("Seules les demandes en attente peuvent etre acceptees");
        }
        if (affectationTransportRepository.existsByEnfantId(demande.getEnfant().getId())) {
            throw new BusinessException("Cet enfant est deja affecte a un transport");
        }

        long nbAffectations = affectationTransportRepository.countByTransportId(transport.getId());
        if (nbAffectations >= transport.getCapacite()) {
            throw new BusinessException("La capacite de ce transport est atteinte");
        }

        AffectationTransport savedAffectation = affectationTransportRepository.save(new AffectationTransport(
                demande.getEnfant(),
                transport,
                demande.getTrajet(),
                demande.getPointRamassage()
        ));

        demande.setStatut(StatutDemandeTransport.ACCEPTEE);
        demandeTransportRepository.save(demande);

        double tauxRemplissage = calculerTauxRemplissage(transportId);
        logger.info("Demande acceptee. demandeId={}, affectationId={}, transportId={}, tauxRemplissage={}",
                demandeId, savedAffectation.getId(), transportId, tauxRemplissage);

        return new TraitementDemandeTransportResponse(
                demande.getId(),
                demande.getStatut(),
                savedAffectation.getId(),
                tauxRemplissage
        );
    }

    @Override
    public TraitementDemandeTransportResponse refuserDemande(Long demandeId) {
        DemandeTransport demande = getDemande(demandeId);
        if (demande.getStatut() != StatutDemandeTransport.EN_ATTENTE) {
            throw new BusinessException("Seules les demandes en attente peuvent etre refusees");
        }

        demande.setStatut(StatutDemandeTransport.REFUSEE);
        demandeTransportRepository.save(demande);
        logger.info("Demande refusee. demandeId={}", demandeId);

        return new TraitementDemandeTransportResponse(
                demande.getId(),
                demande.getStatut(),
                null,
                null
        );
    }

    @Override
    @Transactional
    public TrajetDetailsResponse listerEnfantsParTrajet(Long trajetId) {
        Trajet trajet = getTrajet(trajetId);
        List<EnfantTrajetResponse> enfants = affectationTransportRepository.findByTrajetId(trajetId)
                .stream()
                .map(affectation -> new EnfantTrajetResponse(
                        affectation.getEnfant().getId(),
                        getNomCompletEnfant(affectation.getEnfant()),
                        affectation.getId(),
                        affectation.getTransport().getId(),
                        affectation.getTransport().getNom(),
                        affectation.getTransport().getMatricule(),
                        affectation.getPointRamassage()
                ))
                .toList();

        return new TrajetDetailsResponse(
                trajet.getId(),
                trajet.getPointDepart(),
                trajet.getDestination(),
                trajet.getDateTrajet(),
                trajet.getHeureDepart(),
                enfants.size(),
                enfants
        );
    }

    @Override
    @Transactional
    public double calculerTauxRemplissage(Long transportId) {
        Transport transport = getTransport(transportId);
        if (transport.getCapacite() == null || transport.getCapacite() <= 0) {
            throw new BusinessException("La capacite du transport doit etre superieure a zero");
        }

        long nbAffectations = affectationTransportRepository.countByTransportId(transportId);
        return (nbAffectations * 100.0) / transport.getCapacite();
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable avec l'id " + userId));
    }

    private DemandeTransport getDemande(Long demandeId) {
        return demandeTransportRepository.findById(demandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande de transport introuvable avec l'id " + demandeId));
    }

    private Transport getTransport(Long transportId) {
        return transportRepository.findById(transportId)
                .orElseThrow(() -> new ResourceNotFoundException("Transport introuvable avec l'id " + transportId));
    }

    private Trajet getTrajet(Long trajetId) {
        return trajetRepository.findById(trajetId)
                .orElseThrow(() -> new ResourceNotFoundException("Trajet introuvable avec l'id " + trajetId));
    }

    private DemandeTransportResponse toDemandeResponse(DemandeTransport demande) {
        return new DemandeTransportResponse(
                demande.getId(),
                demande.getEnfant().getId(),
                getNomCompletEnfant(demande.getEnfant()),
                demande.getParent().getId(),
                demande.getParent().getNom(),
                demande.getTrajet().getId(),
                demande.getTrajet().getPointDepart(),
                demande.getTrajet().getDestination(),
                demande.getTrajet().getDateTrajet(),
                demande.getTrajet().getHeureDepart(),
                demande.getStatut(),
                demande.getPointRamassage()
        );
    }

    private String getNomCompletEnfant(Enfant enfant) {
        return enfant.getPrenom() + " " + enfant.getNom();
    }
}
