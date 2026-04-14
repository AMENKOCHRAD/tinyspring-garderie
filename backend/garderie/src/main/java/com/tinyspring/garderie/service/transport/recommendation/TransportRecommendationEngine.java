package com.tinyspring.garderie.service.transport.recommendation;

import com.tinyspring.garderie.config.transport.TransportRecommendationProperties;
import com.tinyspring.garderie.entity.transport.AffectationTransport;
import com.tinyspring.garderie.entity.transport.DemandeTransport;
import com.tinyspring.garderie.entity.transport.SensTrajetDemandeTransport;
import com.tinyspring.garderie.entity.transport.StatutDemandeTransport;
import com.tinyspring.garderie.entity.transport.Trajet;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class TransportRecommendationEngine {

    private final TransportRecommendationProperties properties;

    public TransportRecommendationEngine(TransportRecommendationProperties properties) {
        this.properties = properties;
    }

    public DemandeRecommendationDecision recommendDemande(DemandeTransport demande,
                                                          List<Trajet> candidateTrajets,
                                                          List<DemandeTransport> allDemandes,
                                                          List<AffectationTransport> affectations) {
        ZonePoint demandePoint = resolveDemandePoint(demande);
        if (candidateTrajets.isEmpty()) {
            return new DemandeRecommendationDecision(demande, demandePoint, null, false, 0, null,
                    "Aucun trajet disponible avec de la capacite", "AUCUN_TRAJET");
        }

        List<TrajetCoverage> coverages = buildCoverages(candidateTrajets, allDemandes, affectations);
        RecommendationCandidate bestCandidate = coverages.stream()
                .map(coverage -> buildCandidate(demandePoint, coverage))
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparingInt(RecommendationCandidate::score).reversed()
                        .thenComparing(candidate -> candidate.trajet().getDateTrajet())
                        .thenComparing(candidate -> candidate.trajet().getHeureDepart()))
                .findFirst()
                .orElse(null);

        if (bestCandidate == null) {
            return new DemandeRecommendationDecision(demande, demandePoint, null, false, 0, null,
                    "Destination trop eloignee des trajets existants", "AUCUNE_CORRESPONDANCE");
        }

        boolean autoAssignable = isAutoAssignable(bestCandidate);
        return new DemandeRecommendationDecision(
                demande,
                demandePoint,
                bestCandidate,
                autoAssignable,
                bestCandidate.score(),
                bestCandidate.distanceKm(),
                autoAssignable ? null : buildRefusalReason(bestCandidate),
                bestCandidate.matchMode()
        );
    }

    public List<NouveauTrajetSuggestion> buildNewRouteSuggestions(List<DemandeRecommendationDecision> decisions) {
        List<DemandeRecommendationDecision> rejected = decisions.stream()
                .filter(decision -> !decision.affectationAutomatiquePossible())
                .toList();

        List<NouveauTrajetSuggestion> suggestions = new ArrayList<>();
        suggestions.addAll(buildGeoClusters(rejected));
        suggestions.addAll(buildTextClusters(rejected));

        return suggestions.stream()
                .filter(suggestion -> suggestion.demandes().size() >= properties.getNombreMinimalDemandesPourNouveauTrajet())
                .sorted(Comparator.comparingInt((NouveauTrajetSuggestion item) -> item.demandes().size()).reversed())
                .toList();
    }

    private List<TrajetCoverage> buildCoverages(List<Trajet> trajets,
                                                List<DemandeTransport> allDemandes,
                                                List<AffectationTransport> affectations) {
        Map<Long, DemandeTransport> demandesParEnfant = allDemandes.stream()
                .filter(demande -> demande.getEnfant() != null && demande.getEnfant().getId() != null)
                .collect(Collectors.toMap(
                        demande -> demande.getEnfant().getId(),
                        demande -> demande,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        return trajets.stream()
                .map(trajet -> {
                    List<ZonePoint> coveredPoints = new ArrayList<>();

                    if (trajet.getLatitudeDestination() != null && trajet.getLongitudeDestination() != null) {
                        coveredPoints.add(new ZonePoint(
                                trajet.getZoneDesservie() != null ? trajet.getZoneDesservie() : trajet.getDestination(),
                                trajet.getLatitudeDestination(),
                                trajet.getLongitudeDestination(),
                                "COORDONNEES_TRAJET"
                        ));
                    }

                    affectations.stream()
                            .filter(affectation -> affectation.getTrajet().getId().equals(trajet.getId()))
                            .map(affectation -> demandesParEnfant.get(affectation.getEnfant().getId()))
                            .filter(Objects::nonNull)
                            .filter(demande -> demande.getStatut() == StatutDemandeTransport.ACCEPTEE)
                            .map(this::resolveDemandePoint)
                            .forEach(coveredPoints::add);

                    if (coveredPoints.isEmpty()) {
                        coveredPoints.add(new ZonePoint(
                                trajet.getZoneDesservie() != null ? trajet.getZoneDesservie() : trajet.getDestination(),
                                trajet.getLatitudeDestination(),
                                trajet.getLongitudeDestination(),
                                trajet.getLatitudeDestination() != null && trajet.getLongitudeDestination() != null
                                        ? "COORDONNEES_TRAJET"
                                        : "ZONE_TRAJET"
                        ));
                    }

                    return new TrajetCoverage(trajet, coveredPoints);
                })
                .toList();
    }

    private RecommendationCandidate buildCandidate(ZonePoint demandePoint, TrajetCoverage coverage) {
        RecommendationCandidate best = null;
        for (ZonePoint coveredPoint : coverage.coveredPoints()) {
            RecommendationCandidate current = buildCandidate(demandePoint, coverage.trajet(), coveredPoint);
            if (current == null) {
                continue;
            }
            if (best == null
                    || current.score() > best.score()
                    || (current.score() == best.score() && compareDistance(current.distanceKm(), best.distanceKm()) < 0)) {
                best = current;
            }
        }
        return best;
    }

    private RecommendationCandidate buildCandidate(ZonePoint demandePoint, Trajet trajet, ZonePoint coveredPoint) {
        boolean canUseGeo = demandePoint.latitude() != null
                && demandePoint.longitude() != null
                && coveredPoint.latitude() != null
                && coveredPoint.longitude() != null;

        if (canUseGeo) {
            double distanceKm = haversineKm(
                    demandePoint.latitude(), demandePoint.longitude(), coveredPoint.latitude(), coveredPoint.longitude());
            return new RecommendationCandidate(trajet, calculateGeoScore(distanceKm), round(distanceKm), "DISTANCE_GPS");
        }

        int score = calculateTextScore(demandePoint.zoneLabel(), coveredPoint.zoneLabel());
        return score == 0 ? null : new RecommendationCandidate(trajet, score, null, "SIMILARITE_ZONE");
    }

    private boolean isAutoAssignable(RecommendationCandidate candidate) {
        if ("DISTANCE_GPS".equals(candidate.matchMode())) {
            return candidate.distanceKm() != null && candidate.distanceKm() <= properties.getDistanceMaxKm();
        }
        return candidate.score() >= properties.getScoreTexteMinimal();
    }

    private String buildRefusalReason(RecommendationCandidate candidate) {
        return "DISTANCE_GPS".equals(candidate.matchMode())
                ? "Destination trop eloignee des trajets existants"
                : "Aucune zone de desserte similaire n'a ete trouvee";
    }

    private List<NouveauTrajetSuggestion> buildGeoClusters(List<DemandeRecommendationDecision> rejected) {
        List<DemandeRecommendationDecision> withCoordinates = rejected.stream()
                .filter(decision -> decision.zoneRecherchee().latitude() != null && decision.zoneRecherchee().longitude() != null)
                .toList();

        List<NouveauTrajetSuggestion> clusters = new ArrayList<>();
        Set<Long> visited = new HashSet<>();

        for (DemandeRecommendationDecision decision : withCoordinates) {
            if (!visited.add(decision.demande().getId())) {
                continue;
            }

            List<DemandeRecommendationDecision> cluster = new ArrayList<>();
            ArrayDeque<DemandeRecommendationDecision> queue = new ArrayDeque<>();
            queue.add(decision);

            while (!queue.isEmpty()) {
                DemandeRecommendationDecision current = queue.removeFirst();
                if (cluster.stream().anyMatch(item -> item.demande().getId().equals(current.demande().getId()))) {
                    continue;
                }

                cluster.add(current);
                for (DemandeRecommendationDecision candidate : withCoordinates) {
                    if (visited.contains(candidate.demande().getId())) {
                        continue;
                    }

                    double distance = haversineKm(
                            current.zoneRecherchee().latitude(), current.zoneRecherchee().longitude(),
                            candidate.zoneRecherchee().latitude(), candidate.zoneRecherchee().longitude());
                    if (distance <= properties.getRayonRegroupementKm()) {
                        visited.add(candidate.demande().getId());
                        queue.add(candidate);
                    }
                }
            }

            if (!cluster.isEmpty()) {
                clusters.add(toGeoSuggestion(cluster));
            }
        }

        return clusters;
    }

    private List<NouveauTrajetSuggestion> buildTextClusters(List<DemandeRecommendationDecision> rejected) {
        Map<String, List<DemandeRecommendationDecision>> grouped = new HashMap<>();
        rejected.stream()
                .filter(decision -> decision.zoneRecherchee().latitude() == null || decision.zoneRecherchee().longitude() == null)
                .forEach(decision -> grouped.computeIfAbsent(normalize(decision.zoneRecherchee().zoneLabel()), ignored -> new ArrayList<>())
                        .add(decision));

        return grouped.values().stream()
                .filter(group -> !group.isEmpty())
                .map(this::toTextSuggestion)
                .toList();
    }

    private NouveauTrajetSuggestion toGeoSuggestion(List<DemandeRecommendationDecision> cluster) {
        double latitudeCentre = cluster.stream().map(DemandeRecommendationDecision::zoneRecherchee).mapToDouble(ZonePoint::latitude).average().orElse(0.0);
        double longitudeCentre = cluster.stream().map(DemandeRecommendationDecision::zoneRecherchee).mapToDouble(ZonePoint::longitude).average().orElse(0.0);
        String zoneCentrale = cluster.stream().map(DemandeRecommendationDecision::zoneRecherchee).map(ZonePoint::zoneLabel)
                .filter(label -> label != null && !label.isBlank()).findFirst().orElse("Zone a preciser");
        Double distanceMoyenne = averageDistance(cluster);

        return new NouveauTrajetSuggestion(
                zoneCentrale,
                round(latitudeCentre),
                round(longitudeCentre),
                distanceMoyenne,
                buildRouteSuggestionMessage(cluster.size(), zoneCentrale),
                cluster
        );
    }

    private NouveauTrajetSuggestion toTextSuggestion(List<DemandeRecommendationDecision> group) {
        String zoneCentrale = group.get(0).zoneRecherchee().zoneLabel();
        return new NouveauTrajetSuggestion(
                zoneCentrale,
                null,
                null,
                averageDistance(group),
                buildRouteSuggestionMessage(group.size(), zoneCentrale),
                group
        );
    }

    private Double averageDistance(List<DemandeRecommendationDecision> decisions) {
        double average = decisions.stream()
                .map(DemandeRecommendationDecision::distanceEstimeeKm)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
        return round(average);
    }

    private String buildRouteSuggestionMessage(int size, String zoneCentrale) {
        return "Creer un nouveau trajet pour la zone " + zoneCentrale + " (" + size + " demandes proches non couvertes)";
    }

    private ZonePoint resolveDemandePoint(DemandeTransport demande) {
        String zone = demande.getSensTrajet() == SensTrajetDemandeTransport.GARDERIE_VERS_MAISON
                ? demande.getDestinationSouhaitee()
                : demande.getPointRamassage();
        return new ZonePoint(zone, demande.getLatitudeMaison(), demande.getLongitudeMaison(), "COORDONNEES_DEMANDE");
    }

    private int calculateGeoScore(double distanceKm) {
        double rawScore = 100.0 - ((distanceKm / Math.max(properties.getDistanceMaxKm(), 0.1)) * 100.0);
        return (int) Math.max(0, Math.round(rawScore));
    }

    private int calculateTextScore(String left, String right) {
        String normalizedLeft = normalize(left);
        String normalizedRight = normalize(right);
        if (normalizedLeft.isBlank() || normalizedRight.isBlank()) {
            return 0;
        }
        if (normalizedLeft.equals(normalizedRight)) {
            return 100;
        }
        if (normalizedLeft.contains(normalizedRight) || normalizedRight.contains(normalizedLeft)) {
            return 75;
        }

        List<String> leftTokens = List.of(normalizedLeft.split("\\s+"));
        List<String> rightTokens = List.of(normalizedRight.split("\\s+"));
        long commonTokens = leftTokens.stream()
                .filter(token -> token.length() > 2)
                .filter(rightTokens::contains)
                .distinct()
                .count();

        return commonTokens > 0 ? (int) Math.min(95, commonTokens * 25) : 0;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return earthRadiusKm * (2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
    }

    private Double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private int compareDistance(Double left, Double right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return Double.compare(left, right);
    }

    public record ZonePoint(String zoneLabel, Double latitude, Double longitude, String source) {
    }

    public record RecommendationCandidate(Trajet trajet, int score, Double distanceKm, String matchMode) {
    }

    public record DemandeRecommendationDecision(DemandeTransport demande,
                                                ZonePoint zoneRecherchee,
                                                RecommendationCandidate recommendationCandidate,
                                                boolean affectationAutomatiquePossible,
                                                int scorePertinence,
                                                Double distanceEstimeeKm,
                                                String motifRefus,
                                                String modeEvaluation) {
    }

    public record NouveauTrajetSuggestion(String zoneCentrale,
                                          Double latitudeCentre,
                                          Double longitudeCentre,
                                          Double distanceMoyenneAuTrajetLePlusProcheKm,
                                          String recommandation,
                                          List<DemandeRecommendationDecision> demandes) {
    }

    private record TrajetCoverage(Trajet trajet, List<ZonePoint> coveredPoints) {
    }
}
