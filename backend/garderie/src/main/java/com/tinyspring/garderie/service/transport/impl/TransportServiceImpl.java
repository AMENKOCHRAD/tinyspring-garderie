package com.tinyspring.garderie.service.transport.impl;

import com.tinyspring.garderie.dto.transport.CreateDemandeTransportRequest;
import com.tinyspring.garderie.dto.transport.DemandeTransportResponse;
import com.tinyspring.garderie.dto.transport.EnfantTrajetResponse;
import com.tinyspring.garderie.dto.transport.TrajetDetailsResponse;
import com.tinyspring.garderie.dto.transport.TraitementDemandeTransportResponse;
import com.tinyspring.garderie.dto.transport.UpdateDemandeTransportRequest;
import com.tinyspring.garderie.dto.transport.admin.AdminDemandPredictionResponse;
import com.tinyspring.garderie.dto.transport.parent.ParentEnfantResponse;
import com.tinyspring.garderie.dto.transport.parent.ParentTrajetResponse;
import com.tinyspring.garderie.entity.RoleName;
import com.tinyspring.garderie.entity.User;
import com.tinyspring.garderie.entity.transport.AffectationTransport;
import com.tinyspring.garderie.entity.transport.DemandeTransport;
import com.tinyspring.garderie.entity.transport.Enfant;
import com.tinyspring.garderie.entity.transport.SensTrajetDemandeTransport;
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
import com.tinyspring.garderie.service.transport.ai.AiAnomalyAnalysisResult;
import com.tinyspring.garderie.service.transport.ai.TransportAiService;
import com.tinyspring.garderie.service.transport.recommendation.TransportRecommendationService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
@Transactional
public class TransportServiceImpl implements TransportService {

    private static final Logger logger = LoggerFactory.getLogger(TransportServiceImpl.class);
    private static final String ADRESSE_GARDERIE_EXACTE = "15 Rue des Ecoles, El Menzah 5, Ariana 2091, Tunisie";

    private final DemandeTransportRepository demandeTransportRepository;
    private final AffectationTransportRepository affectationTransportRepository;
    private final EnfantRepository enfantRepository;
    private final TransportRepository transportRepository;
    private final TrajetRepository trajetRepository;
    private final UserRepository userRepository;
    private final TransportRecommendationService transportRecommendationService;
    private final TransportAiService transportAiService;

    public TransportServiceImpl(DemandeTransportRepository demandeTransportRepository,
                                AffectationTransportRepository affectationTransportRepository,
                                EnfantRepository enfantRepository,
                                TransportRepository transportRepository,
                                TrajetRepository trajetRepository,
                                UserRepository userRepository,
                                TransportRecommendationService transportRecommendationService,
                                TransportAiService transportAiService) {
        this.demandeTransportRepository = demandeTransportRepository;
        this.affectationTransportRepository = affectationTransportRepository;
        this.enfantRepository = enfantRepository;
        this.transportRepository = transportRepository;
        this.trajetRepository = trajetRepository;
        this.userRepository = userRepository;
        this.transportRecommendationService = transportRecommendationService;
        this.transportAiService = transportAiService;
    }

    @Override
    public DemandeTransportResponse creerDemandeTransport(Long parentId, CreateDemandeTransportRequest request) {
        User parent = getUser(parentId);
        if (parent.getRole().getName() != RoleName.PARENT) {
            throw new BusinessException("Seul un parent peut effectuer une demande de transport");
        }

        Enfant enfant = enfantRepository.findByIdAndParentId(request.getEnfantId(), parentId)
                .orElseThrow(() -> new BusinessException("Cet enfant n'appartient pas au parent connecte"));
        boolean demandeActive = demandeTransportRepository.existsByEnfantIdAndStatut(enfant.getId(), StatutDemandeTransport.EN_ATTENTE)
                || demandeTransportRepository.existsByEnfantIdAndStatut(enfant.getId(), StatutDemandeTransport.ACCEPTEE);
        if (demandeActive) {
            throw new BusinessException("Une demande active existe deja pour cet enfant");
        }

        DemandeTransport demande = new DemandeTransport(
                enfant,
                parent,
                null,
                StatutDemandeTransport.EN_ATTENTE,
                resolvePointRamassage(request.getSensTrajet(), request.getAdresseMaison()),
                resolveDestinationSouhaitee(request.getSensTrajet(), request.getAdresseMaison()),
                request.getSensTrajet(),
                request.getAdresseMaison().trim(),
                request.getLatitudeMaison(),
                request.getLongitudeMaison(),
                resolveDateSouhaitee(request.getDateSouhaitee()),
                resolveHeureSouhaitee(request.getSensTrajet(), request.getHeureSouhaitee())
        );
        applyAiAnalysis(demande, null);

        DemandeTransport savedDemande = demandeTransportRepository.save(demande);
        logger.info("Demande de transport creee. demandeId={}, enfantId={}, parentId={}",
                savedDemande.getId(), enfant.getId(), parentId);

        return toDemandeResponse(savedDemande);
    }

    @Override
    public DemandeTransportResponse modifierDemandeTransport(Long parentId, Long demandeId, UpdateDemandeTransportRequest request) {
        User parent = getUser(parentId);
        DemandeTransport demande = getDemande(demandeId);

        if (!demande.getParent().getId().equals(parent.getId())) {
            throw new BusinessException("Le parent connecte ne peut modifier que ses propres demandes");
        }
        if (demande.getStatut() != StatutDemandeTransport.EN_ATTENTE) {
            throw new BusinessException("Seules les demandes en attente peuvent etre modifiees");
        }

        Enfant enfant = enfantRepository.findByIdAndParentId(request.getEnfantId(), parentId)
                .orElseThrow(() -> new BusinessException("Cet enfant n'appartient pas au parent connecte"));
        demande.setEnfant(enfant);
        demande.setTrajet(null);
        demande.setSensTrajet(request.getSensTrajet());
        demande.setAdresseMaison(request.getAdresseMaison().trim());
        demande.setLatitudeMaison(request.getLatitudeMaison());
        demande.setLongitudeMaison(request.getLongitudeMaison());
        demande.setDateSouhaitee(resolveDateSouhaitee(request.getDateSouhaitee()));
        demande.setHeureSouhaitee(resolveHeureSouhaitee(request.getSensTrajet(), request.getHeureSouhaitee()));
        demande.setPointRamassage(resolvePointRamassage(request.getSensTrajet(), request.getAdresseMaison()));
        demande.setDestinationSouhaitee(resolveDestinationSouhaitee(request.getSensTrajet(), request.getAdresseMaison()));
        applyAiAnalysis(demande, demandeId);
        logger.info("Demande de transport modifiee. demandeId={}, parentId={}", demandeId, parentId);

        return toDemandeResponse(demandeTransportRepository.save(demande));
    }

    @Override
    public void supprimerDemandeTransport(Long parentId, Long demandeId) {
        DemandeTransport demande = getDemande(demandeId);
        if (!demande.getParent().getId().equals(parentId)) {
            throw new BusinessException("Le parent connecte ne peut supprimer que ses propres demandes");
        }

        if (demande.getStatut() == StatutDemandeTransport.ACCEPTEE) {
            affectationTransportRepository.findByEnfantId(demande.getEnfant().getId())
                    .ifPresent(affectationTransportRepository::delete);
            logger.info("Affectation transport supprimee suite a la suppression parent. demandeId={}, enfantId={}",
                    demandeId, demande.getEnfant().getId());
        }

        demandeTransportRepository.delete(demande);
        logger.info("Demande de transport supprimee. demandeId={}, parentId={}", demandeId, parentId);
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
    @Transactional
    public List<ParentTrajetResponse> listerTrajetsDisponibles() {
        return trajetRepository.findAll().stream()
                .filter(trajet -> trajet.getDateTrajet() != null && !trajet.getDateTrajet().isBefore(LocalDate.now().plusDays(1)))
                .map(trajet -> new ParentTrajetResponse(
                        trajet.getId(),
                        trajet.getPointDepart(),
                        trajet.getDestination(),
                        trajet.getDateTrajet(),
                        trajet.getHeureDepart()
                ))
                .toList();
    }

    @Override
    @Transactional
    public List<ParentEnfantResponse> listerEnfantsParParent(Long parentId) {
        return enfantRepository.findByParentId(parentId).stream()
                .map(enfant -> new ParentEnfantResponse(enfant.getId(), getNomCompletEnfant(enfant)))
                .toList();
    }

    @Override
    public TraitementDemandeTransportResponse accepterDemande(Long demandeId) {
        DemandeTransport demande = getDemande(demandeId);

        if (demande.getStatut() != StatutDemandeTransport.EN_ATTENTE) {
            throw new BusinessException("Seules les demandes en attente peuvent etre acceptees");
        }
        if (affectationTransportRepository.existsByEnfantId(demande.getEnfant().getId())) {
            throw new BusinessException("Cet enfant est deja affecte a un transport");
        }

        Trajet trajet = transportRecommendationService.validerEtRetournerTrajetPourAffectation(demande);
        Transport transport = trajet.getTransport();

        AffectationTransport savedAffectation = affectationTransportRepository.save(new AffectationTransport(
                demande.getEnfant(),
                transport,
                trajet,
                demande.getPointRamassage()
        ));

        demande.setTrajet(trajet);
        demande.setStatut(StatutDemandeTransport.ACCEPTEE);
        demandeTransportRepository.save(demande);

        double tauxRemplissage = calculerTauxRemplissage(transport.getId());
        logger.info("Demande acceptee. demandeId={}, affectationId={}, transportId={}, tauxRemplissage={}",
                demandeId, savedAffectation.getId(), transport.getId(), tauxRemplissage);

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
    public void supprimerDemandeTransportAdmin(Long demandeId) {
        DemandeTransport demande = getDemande(demandeId);

        if (demande.getStatut() == StatutDemandeTransport.ACCEPTEE) {
            affectationTransportRepository.findByEnfantId(demande.getEnfant().getId())
                    .ifPresent(affectationTransportRepository::delete);
            logger.info("Affectation transport supprimee par admin. demandeId={}, enfantId={}",
                    demandeId, demande.getEnfant().getId());
        }

        demandeTransportRepository.delete(demande);
        logger.info("Demande de transport supprimee par admin. demandeId={}", demandeId);
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

    @Override
    public AdminDemandPredictionResponse predireDemandeAdmin(LocalDate targetDate, Integer hour, boolean rainFlag, boolean schoolBreakFlag) {
        return transportAiService.predireDemandePourDashboard(targetDate, hour, rainFlag, schoolBreakFlag);
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
        Trajet trajet = demande.getTrajet();
        return new DemandeTransportResponse(
                demande.getId(),
                demande.getEnfant().getId(),
                getNomCompletEnfant(demande.getEnfant()),
                demande.getParent().getId(),
                demande.getParent().getNom(),
                trajet != null ? trajet.getId() : null,
                demande.getDateDemande(),
                trajet != null ? trajet.getPointDepart() : demande.getPointRamassage(),
                trajet != null ? trajet.getDestination() : demande.getDestinationSouhaitee(),
                trajet != null ? trajet.getDateTrajet() : null,
                trajet != null ? trajet.getHeureDepart() : null,
                demande.getStatut(),
                demande.getPointRamassage(),
                demande.getDestinationSouhaitee(),
                demande.getSensTrajet(),
                demande.getAdresseMaison(),
                demande.getLatitudeMaison(),
                demande.getLongitudeMaison(),
                ADRESSE_GARDERIE_EXACTE,
                demande.getDateSouhaitee(),
                demande.getHeureSouhaitee(),
                demande.getSuspicious(),
                demande.getAnomalyScore(),
                demande.getAnomalyLevel(),
                splitReasons(demande.getAnomalyReasons()),
                demande.getDuplicateDetected(),
                demande.getAiAnalysisAvailable(),
                demande.getAiModelVersion(),
                demande.getAiAnalysisError()
        );
    }

    private String getNomCompletEnfant(Enfant enfant) {
        return enfant.getPrenom() + " " + enfant.getNom();
    }

    private String resolvePointRamassage(SensTrajetDemandeTransport sensTrajet, String adresseMaison) {
        return sensTrajet == SensTrajetDemandeTransport.GARDERIE_VERS_MAISON
                ? ADRESSE_GARDERIE_EXACTE
                : adresseMaison.trim();
    }

    private String resolveDestinationSouhaitee(SensTrajetDemandeTransport sensTrajet, String adresseMaison) {
        return sensTrajet == SensTrajetDemandeTransport.GARDERIE_VERS_MAISON
                ? adresseMaison.trim()
                : ADRESSE_GARDERIE_EXACTE;
    }

    private LocalDate resolveDateSouhaitee(LocalDate dateSouhaitee) {
        return dateSouhaitee != null ? dateSouhaitee : LocalDate.now().plusDays(1);
    }

    private LocalTime resolveHeureSouhaitee(SensTrajetDemandeTransport sensTrajet, LocalTime heureSouhaitee) {
        if (heureSouhaitee != null) {
            return heureSouhaitee;
        }
        return sensTrajet == SensTrajetDemandeTransport.GARDERIE_VERS_MAISON
                ? LocalTime.of(16, 30)
                : LocalTime.of(7, 30);
    }

    private void applyAiAnalysis(DemandeTransport demande, Long currentDemandeId) {
        AiAnomalyAnalysisResult analysis = transportAiService.analyserDemande(demande, currentDemandeId);
        demande.setAiAnalysisAvailable(analysis.aiAvailable());
        demande.setSuspicious(analysis.suspicious());
        demande.setAnomalyScore(analysis.anomalyScore());
        demande.setAnomalyLevel(analysis.anomalyLevel());
        demande.setAnomalyReasons(joinReasons(analysis.anomalyReasons()));
        demande.setDuplicateDetected(analysis.duplicateFound());
        demande.setAiModelVersion(analysis.modelVersion());
        demande.setAiAnalysisError(analysis.errorMessage());

        if (analysis.aiAvailable() && analysis.suspicious()) {
            logger.warn("Demande transport marquee suspecte par l'IA. enfantId={}, score={}, level={}, reasons={}",
                    demande.getEnfant().getId(),
                    analysis.anomalyScore(),
                    analysis.anomalyLevel(),
                    analysis.anomalyReasons());
        }
        if (!analysis.aiAvailable()) {
            logger.warn("Creation/modification poursuivie sans analyse IA. enfantId={}, raison={}",
                    demande.getEnfant().getId(),
                    analysis.errorMessage());
        }
    }

    private String joinReasons(List<String> reasons) {
        if (reasons == null || reasons.isEmpty()) {
            return null;
        }
        return String.join(" | ", reasons);
    }

    private List<String> splitReasons(String reasons) {
        if (reasons == null || reasons.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(reasons.split("\\|"))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }
}
