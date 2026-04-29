package com.tinyspring.garderie.service.transport.recommendation;

import com.tinyspring.garderie.config.transport.TransportRecommendationProperties;
import com.tinyspring.garderie.entity.Role;
import com.tinyspring.garderie.entity.RoleName;
import com.tinyspring.garderie.entity.User;
import com.tinyspring.garderie.entity.transport.DemandeTransport;
import com.tinyspring.garderie.entity.transport.Enfant;
import com.tinyspring.garderie.entity.transport.SensTrajetDemandeTransport;
import com.tinyspring.garderie.entity.transport.StatutDemandeTransport;
import com.tinyspring.garderie.entity.transport.Trajet;
import com.tinyspring.garderie.entity.transport.Transport;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransportRecommendationEngineTest {

    @Test
    void shouldRecommendNearestTrajetByGpsDistance() {
        TransportRecommendationEngine engine = new TransportRecommendationEngine(defaultProperties());
        DemandeTransport demande = buildDemande(1L, 101L, "Zone Ariana", 36.8625, 10.1956);
        Trajet proche = buildRetourTrajet(10L, "Trajet Ariana", 36.8627, 10.1959);
        Trajet loin = buildRetourTrajet(11L, "Trajet La Marsa", 36.8782, 10.3247);

        TransportRecommendationEngine.DemandeRecommendationDecision decision = engine.recommendDemande(
                buildRetourDemande(demande),
                List.of(proche, loin),
                List.of(demande),
                List.of()
        );

        assertTrue(decision.affectationAutomatiquePossible());
        assertEquals(10L, decision.recommendationCandidate().trajet().getId());
    }

    @Test
    void shouldRejectWhenClosestTrajetIsTooFar() {
        TransportRecommendationEngine engine = new TransportRecommendationEngine(defaultProperties());
        DemandeTransport demande = buildDemande(2L, 102L, "Zone Bizerte", 37.2744, 9.8739);
        Trajet trajet = buildRetourTrajet(12L, "Trajet Ariana", 36.8627, 10.1959);

        TransportRecommendationEngine.DemandeRecommendationDecision decision = engine.recommendDemande(
                buildRetourDemande(demande),
                List.of(trajet),
                List.of(demande),
                List.of()
        );

        assertFalse(decision.affectationAutomatiquePossible());
        assertEquals("Destination trop eloignee des trajets existants", decision.motifRefus());
    }

    @Test
    void shouldAcceptTextMatchForSimilarServedZoneNames() {
        TransportRecommendationEngine engine = new TransportRecommendationEngine(defaultProperties());
        DemandeTransport demande = buildTextOnlyDemande(3L, 103L, "Kalaat El Andalous, Ariana, Tunisie");
        Trajet trajet = buildTextOnlyTrajet(13L, "Kalaat el Andalous");

        TransportRecommendationEngine.DemandeRecommendationDecision decision = engine.recommendDemande(
                demande,
                List.of(trajet),
                List.of(demande),
                List.of()
        );

        assertTrue(decision.affectationAutomatiquePossible());
        assertEquals(13L, decision.recommendationCandidate().trajet().getId());
        assertTrue(decision.scorePertinence() >= 60);
    }

    @Test
    void shouldMatchMaisonVersGarderieUsingPointDepartWhenZoneDesservieIsMissing() {
        TransportRecommendationEngine engine = new TransportRecommendationEngine(defaultProperties());
        DemandeTransport demande = buildTextOnlyDemande(4L, 104L, "Bou Hannech, Kalaat El Andalous");
        Trajet trajet = buildTrajetWithPointDepartOnly(14L, "Bou Hannech, Kalaat El Andalous");

        TransportRecommendationEngine.DemandeRecommendationDecision decision = engine.recommendDemande(
                demande,
                List.of(trajet),
                List.of(demande),
                List.of()
        );

        assertTrue(decision.affectationAutomatiquePossible());
        assertEquals(14L, decision.recommendationCandidate().trajet().getId());
        assertEquals("SIMILARITE_ZONE", decision.modeEvaluation());
    }

    @Test
    void shouldUseOutboundTrajetCoordinatesForGpsAutoAssignment() {
        TransportRecommendationEngine engine = new TransportRecommendationEngine(defaultProperties());
        DemandeTransport demande = buildDemande(5L, 105L, "Borj 1, Les Berges du Lac", 36.8432, 10.2728);
        Trajet trajet = buildOutboundTrajetWithCoverageCoordinates(15L, "Borj 1, Les Berges du Lac", 36.8433, 10.2729);

        TransportRecommendationEngine.DemandeRecommendationDecision decision = engine.recommendDemande(
                demande,
                List.of(trajet),
                List.of(demande),
                List.of()
        );

        assertTrue(decision.affectationAutomatiquePossible());
        assertEquals(15L, decision.recommendationCandidate().trajet().getId());
        assertEquals("DISTANCE_GPS", decision.modeEvaluation());
    }

    private TransportRecommendationProperties defaultProperties() {
        TransportRecommendationProperties properties = new TransportRecommendationProperties();
        properties.setDistanceMaxKm(5.0);
        properties.setRayonRegroupementKm(2.0);
        properties.setNombreMinimalDemandesPourNouveauTrajet(2);
        properties.setScoreTexteMinimal(60);
        return properties;
    }

    private DemandeTransport buildDemande(Long demandeId, Long enfantId, String zone, double latitude, double longitude) {
        Role role = new Role(RoleName.PARENT);
        User parent = new User("Parent", "parent" + demandeId + "@test.tn", "secret", true, role);
        setId(parent, 100L + demandeId);

        Enfant enfant = new Enfant("Doe", "Jane", parent);
        setId(enfant, enfantId);

        DemandeTransport demande = new DemandeTransport(
                enfant,
                parent,
                null,
                StatutDemandeTransport.EN_ATTENTE,
                zone,
                "Garderie",
                SensTrajetDemandeTransport.MAISON_VERS_GARDERIE,
                zone,
                latitude,
                longitude,
                LocalDate.now().plusDays(1),
                LocalTime.of(7, 30)
        );
        setId(demande, demandeId);
        return demande;
    }

    private DemandeTransport buildRetourDemande(DemandeTransport demande) {
        demande.setSensTrajet(SensTrajetDemandeTransport.GARDERIE_VERS_MAISON);
        demande.setDestinationSouhaitee(demande.getPointRamassage());
        return demande;
    }

    private Trajet buildRetourTrajet(Long id, String zone, double latitude, double longitude) {
        Transport transport = new Transport("Bus " + id, "TR-" + id, 12);
        setId(transport, 500L + id);

        Trajet trajet = new Trajet(
                "15 Rue des Ecoles, El Menzah 5, Ariana 2091, Tunisie",
                zone,
                LocalDate.now().plusDays(1),
                LocalTime.of(8, 0),
                transport
        );
        trajet.setZoneDesservie(zone);
        trajet.setLatitudeDestination(latitude);
        trajet.setLongitudeDestination(longitude);
        setId(trajet, id);
        return trajet;
    }

    private DemandeTransport buildTextOnlyDemande(Long demandeId, Long enfantId, String zone) {
        DemandeTransport demande = buildDemande(demandeId, enfantId, zone, 36.85, 10.20);
        demande.setLatitudeMaison(null);
        demande.setLongitudeMaison(null);
        return demande;
    }

    private Trajet buildTextOnlyTrajet(Long id, String zone) {
        Transport transport = new Transport("Bus " + id, "TR-" + id, 12);
        setId(transport, 500L + id);

        Trajet trajet = new Trajet("Point " + id, "Garderie", LocalDate.now().plusDays(1), LocalTime.of(8, 0), transport);
        trajet.setZoneDesservie(zone);
        trajet.setLatitudeDestination(null);
        trajet.setLongitudeDestination(null);
        setId(trajet, id);
        return trajet;
    }

    private Trajet buildTrajetWithPointDepartOnly(Long id, String pointDepart) {
        Transport transport = new Transport("Bus " + id, "TR-" + id, 12);
        setId(transport, 500L + id);

        Trajet trajet = new Trajet(
                pointDepart,
                "15 Rue des Ecoles, El Menzah 5, Ariana 2091, Tunisie",
                LocalDate.now().plusDays(1),
                LocalTime.of(8, 0),
                transport
        );
        trajet.setZoneDesservie(null);
        trajet.setLatitudeDestination(36.8065);
        trajet.setLongitudeDestination(10.1815);
        setId(trajet, id);
        return trajet;
    }

    private Trajet buildOutboundTrajetWithCoverageCoordinates(Long id, String pointDepart, double latitude, double longitude) {
        Transport transport = new Transport("Bus " + id, "TR-" + id, 12);
        setId(transport, 500L + id);

        Trajet trajet = new Trajet(
                pointDepart,
                "15 Rue des Ecoles, El Menzah 5, Ariana 2091, Tunisie",
                LocalDate.now().plusDays(1),
                LocalTime.of(8, 0),
                transport
        );
        trajet.setZoneDesservie(pointDepart);
        trajet.setLatitudeDestination(latitude);
        trajet.setLongitudeDestination(longitude);
        setId(trajet, id);
        return trajet;
    }

    private void setId(Object target, Long id) {
        try {
            Field field = target.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
