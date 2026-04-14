package com.tinyspring.garderie.service.transport.ai.impl;

import com.tinyspring.garderie.config.transport.TransportAiProperties;
import com.tinyspring.garderie.dto.transport.admin.AdminDemandPredictionResponse;
import com.tinyspring.garderie.dto.transport.ai.AiDetectAnomalyRequest;
import com.tinyspring.garderie.dto.transport.ai.AiDetectAnomalyResponse;
import com.tinyspring.garderie.dto.transport.ai.AiExistingRequestDto;
import com.tinyspring.garderie.dto.transport.ai.AiPredictDemandRequest;
import com.tinyspring.garderie.dto.transport.ai.AiPredictDemandResponse;
import com.tinyspring.garderie.entity.transport.DemandeTransport;
import com.tinyspring.garderie.repository.transport.DemandeTransportRepository;
import com.tinyspring.garderie.repository.transport.EnfantRepository;
import com.tinyspring.garderie.repository.transport.TrajetRepository;
import com.tinyspring.garderie.repository.transport.TransportRepository;
import com.tinyspring.garderie.service.transport.ai.AiAnomalyAnalysisResult;
import com.tinyspring.garderie.service.transport.ai.TransportAiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Service
public class TransportAiServiceImpl implements TransportAiService {

    private static final Logger logger = LoggerFactory.getLogger(TransportAiServiceImpl.class);
    private static final double DEFAULT_ROUTE_DISTANCE_KM = 5.0;
    private static final double GARDERIE_LATITUDE = 36.8065;
    private static final double GARDERIE_LONGITUDE = 10.1815;

    private final WebClient transportAiWebClient;
    private final TransportAiProperties properties;
    private final DemandeTransportRepository demandeTransportRepository;
    private final EnfantRepository enfantRepository;
    private final TransportRepository transportRepository;
    private final TrajetRepository trajetRepository;

    public TransportAiServiceImpl(WebClient transportAiWebClient,
                                  TransportAiProperties properties,
                                  DemandeTransportRepository demandeTransportRepository,
                                  EnfantRepository enfantRepository,
                                  TransportRepository transportRepository,
                                  TrajetRepository trajetRepository) {
        this.transportAiWebClient = transportAiWebClient;
        this.properties = properties;
        this.demandeTransportRepository = demandeTransportRepository;
        this.enfantRepository = enfantRepository;
        this.transportRepository = transportRepository;
        this.trajetRepository = trajetRepository;
    }

    @Override
    public AiAnomalyAnalysisResult analyserDemande(DemandeTransport demande, Long currentDemandeId) {
        try {
            AiDetectAnomalyRequest payload = new AiDetectAnomalyRequest(
                    demande.getEnfant().getId(),
                    demande.getDateSouhaitee(),
                    demande.getHeureSouhaitee().getHour(),
                    demande.getPointRamassage(),
                    demande.getDestinationSouhaitee(),
                    1,
                    estimateRouteDistanceKm(demande),
                    false,
                    buildRecentRequests(demande, currentDemandeId)
            );

            AiDetectAnomalyResponse response = transportAiWebClient.post()
                    .uri("/detect-anomaly")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(AiDetectAnomalyResponse.class)
                    .block(Duration.ofSeconds(properties.getTimeoutSeconds()));

            if (response == null) {
                return unavailableAnalysis("Le microservice IA a retourne une reponse vide.");
            }

            return new AiAnomalyAnalysisResult(
                    true,
                    response.anomaly(),
                    response.anomalyScore(),
                    response.anomalyLevel(),
                    response.reasons() == null ? List.of() : response.reasons(),
                    response.duplicateFound(),
                    response.modelVersion(),
                    null
            );
        } catch (Exception ex) {
            logger.warn("Analyse IA indisponible pour la demande transport. enfantId={}, error={}",
                    demande.getEnfant().getId(), ex.getMessage());
            return unavailableAnalysis("Microservice IA indisponible: " + ex.getMessage());
        }
    }

    @Override
    public AdminDemandPredictionResponse predireDemandePourDashboard(LocalDate targetDate,
                                                                     Integer hour,
                                                                     boolean rainFlag,
                                                                     boolean schoolBreakFlag) {
        LocalDate effectiveDate = targetDate != null ? targetDate : LocalDate.now().plusDays(1);
        int effectiveHour = hour != null ? hour : 8;
        long activeChildrenCount = enfantRepository.count();
        long availableTransportCount = transportRepository.count();
        double avgRouteDistanceKm = computeAverageRouteDistanceKm();

        try {
            AiPredictDemandResponse response = transportAiWebClient.post()
                    .uri("/predict-demand")
                    .bodyValue(new AiPredictDemandRequest(
                            effectiveDate,
                            effectiveHour,
                            activeChildrenCount,
                            availableTransportCount,
                            avgRouteDistanceKm,
                            rainFlag,
                            schoolBreakFlag
                    ))
                    .retrieve()
                    .bodyToMono(AiPredictDemandResponse.class)
                    .block(Duration.ofSeconds(properties.getTimeoutSeconds()));

            if (response == null) {
                return new AdminDemandPredictionResponse(
                        effectiveDate,
                        effectiveHour,
                        activeChildrenCount,
                        availableTransportCount,
                        avgRouteDistanceKm,
                        null,
                        "UNKNOWN",
                        false,
                        null,
                        "Le microservice IA a retourne une reponse vide."
                );
            }

            return new AdminDemandPredictionResponse(
                    effectiveDate,
                    effectiveHour,
                    activeChildrenCount,
                    availableTransportCount,
                    avgRouteDistanceKm,
                    response.predictedDemandCount(),
                    response.demandLevel(),
                    true,
                    response.modelVersion(),
                    "Prediction generee avec succes."
            );
        } catch (Exception ex) {
            logger.warn("Prediction de demande indisponible pour le dashboard admin: {}", ex.getMessage());
            return new AdminDemandPredictionResponse(
                    effectiveDate,
                    effectiveHour,
                    activeChildrenCount,
                    availableTransportCount,
                    avgRouteDistanceKm,
                    null,
                    "UNKNOWN",
                    false,
                    null,
                    "Microservice IA indisponible: " + ex.getMessage()
            );
        }
    }

    private List<AiExistingRequestDto> buildRecentRequests(DemandeTransport demande, Long currentDemandeId) {
        return demandeTransportRepository.findTop10ByEnfantIdOrderByIdDesc(demande.getEnfant().getId())
                .stream()
                .filter(existing -> currentDemandeId == null || !existing.getId().equals(currentDemandeId))
                .map(existing -> new AiExistingRequestDto(
                        existing.getEnfant().getId(),
                        existing.getDateSouhaitee(),
                        existing.getHeureSouhaitee() != null ? existing.getHeureSouhaitee().getHour() : 8,
                        existing.getPointRamassage(),
                        existing.getDestinationSouhaitee()
                ))
                .toList();
    }

    private double estimateRouteDistanceKm(DemandeTransport demande) {
        if (demande.getLatitudeMaison() == null || demande.getLongitudeMaison() == null) {
            return DEFAULT_ROUTE_DISTANCE_KM;
        }

        return haversineKm(
                demande.getLatitudeMaison(),
                demande.getLongitudeMaison(),
                GARDERIE_LATITUDE,
                GARDERIE_LONGITUDE
        );
    }

    private double computeAverageRouteDistanceKm() {
        return trajetRepository.findAll().stream()
                .filter(trajet -> trajet.getLatitudeDestination() != null && trajet.getLongitudeDestination() != null)
                .mapToDouble(trajet -> haversineKm(
                        GARDERIE_LATITUDE,
                        GARDERIE_LONGITUDE,
                        trajet.getLatitudeDestination(),
                        trajet.getLongitudeDestination()
                ))
                .average()
                .orElse(DEFAULT_ROUTE_DISTANCE_KM);
    }

    private AiAnomalyAnalysisResult unavailableAnalysis(String errorMessage) {
        return new AiAnomalyAnalysisResult(
                false,
                false,
                null,
                "UNKNOWN",
                Collections.emptyList(),
                false,
                null,
                errorMessage
        );
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double originLat = Math.toRadians(lat1);
        double destinationLat = Math.toRadians(lat2);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(originLat) * Math.cos(destinationLat);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }
}
