package com.tinyspring.garderie.service.transport.recommendation.impl;

import com.tinyspring.garderie.config.transport.TransportRecommendationProperties;
import com.tinyspring.garderie.entity.Role;
import com.tinyspring.garderie.entity.RoleName;
import com.tinyspring.garderie.entity.User;
import com.tinyspring.garderie.entity.transport.DemandeTransport;
import com.tinyspring.garderie.entity.transport.Enfant;
import com.tinyspring.garderie.entity.transport.AffectationTransport;
import com.tinyspring.garderie.entity.transport.SensTrajetDemandeTransport;
import com.tinyspring.garderie.entity.transport.StatutDemandeTransport;
import com.tinyspring.garderie.entity.transport.Trajet;
import com.tinyspring.garderie.entity.transport.Transport;
import com.tinyspring.garderie.exception.BusinessException;
import com.tinyspring.garderie.repository.transport.AffectationTransportRepository;
import com.tinyspring.garderie.repository.transport.DemandeTransportRepository;
import com.tinyspring.garderie.repository.transport.TrajetRepository;
import com.tinyspring.garderie.service.transport.recommendation.TransportRecommendationEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransportRecommendationServiceImplTest {

    @Mock
    private DemandeTransportRepository demandeTransportRepository;

    @Mock
    private AffectationTransportRepository affectationTransportRepository;

    @Mock
    private TrajetRepository trajetRepository;

    @Spy
    private TransportRecommendationEngine recommendationEngine = new TransportRecommendationEngine(defaultProperties());

    @InjectMocks
    private TransportRecommendationServiceImpl recommendationService;

    @Test
    void shouldRecommendTrajetMatchingRequestedDateAndSense() {
        DemandeTransport demande = buildDemande(1L, "Kalaat El Andalous, Ariana, Tunisie", LocalDate.now().plusDays(1));
        Trajet goodTrajet = buildTrajet(
                10L,
                "Bou Hannech, Delegation Kalaat El Andalous, Gouvernorat Ariana, Tunisie",
                "15 Rue des Ecoles, El Menzah 5, Ariana 2091, Tunisie",
                "Kalâat el-Andalous",
                LocalDate.now().plusDays(1)
        );
        Trajet wrongDateTrajet = buildTrajet(
                11L,
                "Bou Hannech, Delegation Kalaat El Andalous, Gouvernorat Ariana, Tunisie",
                "15 Rue des Ecoles, El Menzah 5, Ariana 2091, Tunisie",
                "Kalâat el-Andalous",
                LocalDate.now()
        );
        Trajet wrongSenseTrajet = buildTrajet(
                12L,
                "15 Rue des Ecoles, El Menzah 5, Ariana 2091, Tunisie",
                "Kalaat El Andalous, Ariana, Tunisie",
                "Kalâat el-Andalous",
                LocalDate.now().plusDays(1)
        );

        when(demandeTransportRepository.findAllByOrderByIdDesc()).thenReturn(List.of(demande));
        when(affectationTransportRepository.findAll()).thenReturn(List.of());
        when(trajetRepository.findByDateTrajetGreaterThanEqualOrderByDateTrajetAscHeureDepartAsc(LocalDate.now()))
                .thenReturn(List.of(goodTrajet, wrongDateTrajet, wrongSenseTrajet));

        TransportRecommendationEngine.DemandeRecommendationDecision decision = recommendationService.recommanderPourDemande(demande);

        assertTrue(decision.affectationAutomatiquePossible());
        assertEquals(10L, decision.recommendationCandidate().trajet().getId());
    }

    @Test
    void shouldRecommendTrajetForSameDayRequestWhenDateMatchesToday() {
        DemandeTransport demande = buildDemande(2L, "Borj 1, Les Berges du Lac, Tunis", LocalDate.now());
        Trajet sameDayTrajet = buildTrajet(
                20L,
                "Borj 1, Les Berges du Lac, Tunis",
                "15 Rue des Ecoles, El Menzah 5, Ariana 2091, Tunisie",
                null,
                LocalDate.now()
        );

        when(demandeTransportRepository.findAllByOrderByIdDesc()).thenReturn(List.of(demande));
        when(affectationTransportRepository.findAll()).thenReturn(List.of());
        when(trajetRepository.findByDateTrajetGreaterThanEqualOrderByDateTrajetAscHeureDepartAsc(LocalDate.now()))
                .thenReturn(List.of(sameDayTrajet));

        TransportRecommendationEngine.DemandeRecommendationDecision decision = recommendationService.recommanderPourDemande(demande);

        assertTrue(decision.affectationAutomatiquePossible());
        assertEquals(20L, decision.recommendationCandidate().trajet().getId());
    }

    @Test
    void shouldReturnAffectationRecommendationsOnlyForEligibleDemandes() {
        DemandeTransport eligible = buildDemande(30L, "Kalaat El Andalous", LocalDate.now().plusDays(1));
        DemandeTransport refused = buildDemande(31L, "Kalaat El Andalous", LocalDate.now().plusDays(1));
        refused.setStatut(StatutDemandeTransport.REFUSEE);
        DemandeTransport withTrajet = buildDemande(32L, "Kalaat El Andalous", LocalDate.now().plusDays(1));
        withTrajet.setTrajet(buildTrajet(90L, "Kalaat", "Garderie", "Kalaat", LocalDate.now().plusDays(1)));
        DemandeTransport alreadyAffected = buildDemande(33L, "Kalaat El Andalous", LocalDate.now().plusDays(1));

        Trajet availableTrajet = buildTrajet(
                40L,
                "Kalaat El Andalous",
                "15 Rue des Ecoles, El Menzah 5, Ariana 2091, Tunisie",
                "Kalaat El Andalous",
                LocalDate.now().plusDays(1)
        );
        AffectationTransport affectation = new AffectationTransport(
                alreadyAffected.getEnfant(),
                availableTrajet.getTransport(),
                availableTrajet,
                "Kalaat El Andalous"
        );
        setId(affectation, 500L);

        when(demandeTransportRepository.findAllByOrderByIdDesc()).thenReturn(List.of(eligible, refused, withTrajet, alreadyAffected));
        when(affectationTransportRepository.findAll()).thenReturn(List.of(affectation));
        when(trajetRepository.findByDateTrajetGreaterThanEqualOrderByDateTrajetAscHeureDepartAsc(LocalDate.now()))
                .thenReturn(List.of(availableTrajet));

        List<com.tinyspring.garderie.dto.transport.admin.DemandeAffectationRecommendationResponse> responses =
                recommendationService.getRecommendationsAffectation();

        assertEquals(1, responses.size());
        assertEquals(30L, responses.get(0).demandeId());
        assertEquals(40L, responses.get(0).trajetRecommandeId());
    }

    @Test
    void shouldSuggestNewRoutesForRejectedDemandGroups() {
        DemandeTransport demande1 = buildTextOnlyDemande(50L, 250L, "Marsa Ville");
        DemandeTransport demande2 = buildTextOnlyDemande(51L, 251L, "Marsa Ville");

        when(demandeTransportRepository.findAllByOrderByIdDesc()).thenReturn(List.of(demande1, demande2));
        when(affectationTransportRepository.findAll()).thenReturn(List.of());
        when(trajetRepository.findByDateTrajetGreaterThanEqualOrderByDateTrajetAscHeureDepartAsc(LocalDate.now()))
                .thenReturn(List.of());

        List<com.tinyspring.garderie.dto.transport.admin.NouveauTrajetRecommendationResponse> responses =
                recommendationService.getRecommendationsNouveauxTrajets();

        assertEquals(1, responses.size());
        assertEquals("Marsa Ville", responses.get(0).zoneCentrale());
        assertEquals(2, responses.get(0).nombreDemandes());
        assertEquals(2, responses.get(0).demandes().size());
    }

    @Test
    void shouldThrowWhenAutomaticAffectationIsImpossible() {
        DemandeTransport demande = buildTextOnlyDemande(60L, 260L, "Bizerte");

        when(demandeTransportRepository.findAllByOrderByIdDesc()).thenReturn(List.of(demande));
        when(affectationTransportRepository.findAll()).thenReturn(List.of());
        when(trajetRepository.findByDateTrajetGreaterThanEqualOrderByDateTrajetAscHeureDepartAsc(LocalDate.now()))
                .thenReturn(List.of());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> recommendationService.validerEtRetournerTrajetPourAffectation(demande)
        );

        assertEquals("Aucun trajet disponible avec de la capacite", exception.getMessage());
    }

    @Test
    void shouldReturnRecommendedTrajetForAutomaticAffectation() {
        DemandeTransport demande = buildDemande(61L, "Borj 1, Les Berges du Lac, Tunis", LocalDate.now().plusDays(1));
        Trajet trajet = buildTrajet(
                62L,
                "Borj 1, Les Berges du Lac, Tunis",
                "15 Rue des Ecoles, El Menzah 5, Ariana 2091, Tunisie",
                "Borj 1, Les Berges du Lac, Tunis",
                LocalDate.now().plusDays(1)
        );

        when(demandeTransportRepository.findAllByOrderByIdDesc()).thenReturn(List.of(demande));
        when(affectationTransportRepository.findAll()).thenReturn(List.of());
        when(trajetRepository.findByDateTrajetGreaterThanEqualOrderByDateTrajetAscHeureDepartAsc(LocalDate.now()))
                .thenReturn(List.of(trajet));

        Trajet recommended = recommendationService.validerEtRetournerTrajetPourAffectation(demande);

        assertEquals(62L, recommended.getId());
    }

    private static TransportRecommendationProperties defaultProperties() {
        TransportRecommendationProperties properties = new TransportRecommendationProperties();
        properties.setDistanceMaxKm(5.0);
        properties.setRayonRegroupementKm(2.0);
        properties.setNombreMinimalDemandesPourNouveauTrajet(2);
        properties.setScoreTexteMinimal(60);
        return properties;
    }

    private DemandeTransport buildDemande(Long id, String zone, LocalDate dateSouhaitee) {
        Role role = new Role(RoleName.PARENT);
        User parent = new User("Parent", "parent" + id + "@test.tn", "secret", true, role);
        setId(parent, 100L + id);

        Enfant enfant = new Enfant("Doe", "Jane", parent);
        setId(enfant, 200L + id);

        DemandeTransport demande = new DemandeTransport(
                enfant,
                parent,
                null,
                StatutDemandeTransport.EN_ATTENTE,
                zone,
                "15 Rue des Ecoles, El Menzah 5, Ariana 2091, Tunisie",
                SensTrajetDemandeTransport.MAISON_VERS_GARDERIE,
                zone,
                null,
                null,
                dateSouhaitee,
                LocalTime.of(7, 30)
        );
        setId(demande, id);
        return demande;
    }

    private Trajet buildTrajet(Long id, String pointDepart, String destination, String zoneDesservie, LocalDate dateTrajet) {
        Transport transport = new Transport("Bus " + id, "TN-" + id, 12);
        setId(transport, 300L + id);

        Trajet trajet = new Trajet(pointDepart, destination, dateTrajet, LocalTime.of(7, 45), transport);
        trajet.setZoneDesservie(zoneDesservie);
        setId(trajet, id);
        return trajet;
    }

    private DemandeTransport buildTextOnlyDemande(Long id, Long enfantId, String zone) {
        Role role = new Role(RoleName.PARENT);
        User parent = new User("Parent", "parent" + id + "@test.tn", "secret", true, role);
        setId(parent, 100L + id);

        Enfant enfant = new Enfant("Doe", "Jane", parent);
        setId(enfant, enfantId);

        DemandeTransport demande = new DemandeTransport(
                enfant,
                parent,
                null,
                StatutDemandeTransport.EN_ATTENTE,
                zone,
                "15 Rue des Ecoles, El Menzah 5, Ariana 2091, Tunisie",
                SensTrajetDemandeTransport.MAISON_VERS_GARDERIE,
                zone,
                null,
                null,
                LocalDate.now().plusDays(1),
                LocalTime.of(7, 30)
        );
        setId(demande, id);
        return demande;
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
