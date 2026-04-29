package com.tinyspring.garderie.coverage;
import com.tinyspring.garderie.config.PasswordConfig;
import com.tinyspring.garderie.config.transport.TransportAiClientConfig;
import com.tinyspring.garderie.config.transport.TransportAiProperties;
import com.tinyspring.garderie.config.transport.TransportRecommendationProperties;
import com.tinyspring.garderie.dto.transport.ai.AiExistingRequestDto;
import com.tinyspring.garderie.dto.transport.admin.NouveauTrajetSuggestionDemandeResponse;
import com.tinyspring.garderie.entity.Role;
import com.tinyspring.garderie.entity.RoleName;
import com.tinyspring.garderie.entity.User;
import com.tinyspring.garderie.entity.transport.AffectationTransport;
import com.tinyspring.garderie.entity.transport.DemandeTransport;
import com.tinyspring.garderie.entity.transport.Enfant;
import com.tinyspring.garderie.entity.transport.SensTrajetDemandeTransport;
import com.tinyspring.garderie.entity.transport.StatutDemandeTransport;
import com.tinyspring.garderie.entity.transport.Trajet;
import com.tinyspring.garderie.entity.transport.Transport;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.reactive.function.client.WebClient;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityAndDtoCoverageTest {

    @Test
    void shouldCoverRecommendationAndAiProperties() {
        TransportRecommendationProperties recommendationProperties = new TransportRecommendationProperties();
        recommendationProperties.setDistanceMaxKm(7.5);
        recommendationProperties.setRayonRegroupementKm(3.2);
        recommendationProperties.setNombreMinimalDemandesPourNouveauTrajet(4);
        recommendationProperties.setScoreTexteMinimal(70);

        assertEquals(7.5, recommendationProperties.getDistanceMaxKm());
        assertEquals(3.2, recommendationProperties.getRayonRegroupementKm());
        assertEquals(4, recommendationProperties.getNombreMinimalDemandesPourNouveauTrajet());
        assertEquals(70, recommendationProperties.getScoreTexteMinimal());

        TransportAiProperties aiProperties = new TransportAiProperties();
        aiProperties.setBaseUrl("http://localhost:9000");
        aiProperties.setTimeoutSeconds(9);

        assertEquals("http://localhost:9000", aiProperties.getBaseUrl());
        assertEquals(9, aiProperties.getTimeoutSeconds());
    }

    @Test
    void shouldCreatePasswordEncoderAndWebClient() {
        PasswordEncoder encoder = new PasswordConfig().passwordEncoder();
        assertTrue(encoder.matches("secret", encoder.encode("secret")));

        TransportAiProperties properties = new TransportAiProperties();
        properties.setBaseUrl("http://localhost:8001");
        WebClient client = new TransportAiClientConfig().transportAiWebClient(properties);
        assertNotNull(client);
    }

    @Test
    void shouldCoverRoleUserAndEnfantEntities() throws Exception {
        Role role = new Role(RoleName.PARENT);
        setId(role, 1L);
        role.setName(RoleName.ADMIN);

        User user = new User("Nom", "mail@test.tn", "pwd", true, role);
        setId(user, 2L);
        user.setNom("Autre Nom");
        user.setEmail("autre@test.tn");
        user.setPassword("encoded");
        user.setEnabled(false);
        user.setRole(role);

        Enfant enfant = new Enfant("Doe", "Jane", user);
        setId(enfant, 3L);
        enfant.setNom("Smith");
        enfant.setPrenom("Lina");
        enfant.setParent(user);

        assertEquals(1L, role.getId());
        assertEquals(RoleName.ADMIN, role.getName());
        assertEquals(2L, user.getId());
        assertEquals("Autre Nom", user.getNom());
        assertEquals("autre@test.tn", user.getEmail());
        assertEquals("encoded", user.getPassword());
        assertEquals(false, user.isEnabled());
        assertEquals(role, user.getRole());
        assertEquals(3L, enfant.getId());
        assertEquals("Smith", enfant.getNom());
        assertEquals("Lina", enfant.getPrenom());
        assertEquals(user, enfant.getParent());
    }

    @Test
    void shouldCoverTransportTrajetAndAffectationEntities() throws Exception {
        Transport transport = new Transport("Bus A", "TN-101", 12);
        setId(transport, 10L);
        transport.setNom("Bus B");
        transport.setMatricule("TN-102");
        transport.setCapacite(14);

        Trajet trajet = new Trajet("Ariana", "Garderie", LocalDate.of(2026, 5, 1), LocalTime.of(7, 30), transport);
        setId(trajet, 11L);
        trajet.setPointDepart("Marsa");
        trajet.setDestination("Menzah");
        trajet.setZoneDesservie("Marsa Ville");
        trajet.setLatitudeDestination(36.9);
        trajet.setLongitudeDestination(10.3);
        trajet.setDateTrajet(LocalDate.of(2026, 5, 2));
        trajet.setHeureDepart(LocalTime.of(8, 15));
        trajet.setTransport(transport);

        User parent = new User("Parent", "parent@test.tn", "pwd", true, new Role(RoleName.PARENT));
        Enfant enfant = new Enfant("Doe", "Aya", parent);
        AffectationTransport affectation = new AffectationTransport(enfant, transport, trajet, "Marsa");
        setId(affectation, 12L);
        affectation.setEnfant(enfant);
        affectation.setTransport(transport);
        affectation.setTrajet(trajet);
        affectation.setPointRamassage("Marsa Ville");

        assertEquals(10L, transport.getId());
        assertEquals("Bus B", transport.getNom());
        assertEquals("TN-102", transport.getMatricule());
        assertEquals(14, transport.getCapacite());
        assertEquals(11L, trajet.getId());
        assertEquals("Marsa", trajet.getPointDepart());
        assertEquals("Menzah", trajet.getDestination());
        assertEquals("Marsa Ville", trajet.getZoneDesservie());
        assertEquals(36.9, trajet.getLatitudeDestination());
        assertEquals(10.3, trajet.getLongitudeDestination());
        assertEquals(LocalDate.of(2026, 5, 2), trajet.getDateTrajet());
        assertEquals(LocalTime.of(8, 15), trajet.getHeureDepart());
        assertEquals(transport, trajet.getTransport());
        assertEquals(12L, affectation.getId());
        assertEquals(enfant, affectation.getEnfant());
        assertEquals(transport, affectation.getTransport());
        assertEquals(trajet, affectation.getTrajet());
        assertEquals("Marsa Ville", affectation.getPointRamassage());
    }

    @Test
    void shouldCoverDemandeTransportEntityAndRecords() throws Exception {
        Role role = new Role(RoleName.PARENT);
        User parent = new User("Parent", "parent@test.tn", "pwd", true, role);
        Enfant enfant = new Enfant("Doe", "Sami", parent);
        Transport transport = new Transport("Bus", "TN-200", 10);
        Trajet trajet = new Trajet("Ariana", "Garderie", LocalDate.of(2026, 5, 1), LocalTime.of(7, 30), transport);

        DemandeTransport demande = new DemandeTransport(
                enfant,
                parent,
                trajet,
                StatutDemandeTransport.EN_ATTENTE,
                "Ariana",
                "Garderie",
                SensTrajetDemandeTransport.MAISON_VERS_GARDERIE,
                "Rue 1",
                36.8,
                10.1,
                LocalDate.of(2026, 5, 3),
                LocalTime.of(7, 45)
        );
        setId(demande, 20L);
        demande.setEnfant(enfant);
        demande.setParent(parent);
        demande.setTrajet(trajet);
        demande.setDateDemande(LocalDate.of(2026, 4, 29));
        demande.setStatut(StatutDemandeTransport.REVISION_PARENT_DEMANDEE);
        demande.setPointRamassage("Menzah");
        demande.setDestinationSouhaitee("Garderie Centrale");
        demande.setSensTrajet(SensTrajetDemandeTransport.GARDERIE_VERS_MAISON);
        demande.setAdresseMaison("Rue 2");
        demande.setLatitudeMaison(36.81);
        demande.setLongitudeMaison(10.11);
        demande.setDateSouhaitee(LocalDate.of(2026, 5, 4));
        demande.setHeureSouhaitee(LocalTime.of(16, 30));
        demande.setSuspicious(true);
        demande.setAiAnalysisAvailable(true);
        demande.setDuplicateDetected(true);
        demande.setAnomalyScore(0.91);
        demande.setAnomalyLevel("HIGH");
        demande.setAnomalyReasons("Adresse inhabituelle");
        demande.setAiModelVersion("v-test");
        demande.setAiAnalysisError("none");
        demande.setRevisionRequestMessage("Merci de confirmer");
        demande.setRevisionRequestedAt(LocalDateTime.of(2026, 4, 29, 10, 15));

        AiExistingRequestDto aiExistingRequestDto = new AiExistingRequestDto(
                100L,
                LocalDate.of(2026, 5, 4),
                16,
                "Rue 2",
                "Garderie Centrale"
        );
        NouveauTrajetSuggestionDemandeResponse suggestion = new NouveauTrajetSuggestionDemandeResponse(
                20L,
                30L,
                "Sami Doe",
                SensTrajetDemandeTransport.GARDERIE_VERS_MAISON,
                "Menzah",
                "Garderie Centrale"
        );

        assertEquals(20L, demande.getId());
        assertEquals(enfant, demande.getEnfant());
        assertEquals(parent, demande.getParent());
        assertEquals(trajet, demande.getTrajet());
        assertEquals(LocalDate.of(2026, 4, 29), demande.getDateDemande());
        assertEquals(StatutDemandeTransport.REVISION_PARENT_DEMANDEE, demande.getStatut());
        assertEquals("Menzah", demande.getPointRamassage());
        assertEquals("Garderie Centrale", demande.getDestinationSouhaitee());
        assertEquals(SensTrajetDemandeTransport.GARDERIE_VERS_MAISON, demande.getSensTrajet());
        assertEquals("Rue 2", demande.getAdresseMaison());
        assertEquals(36.81, demande.getLatitudeMaison());
        assertEquals(10.11, demande.getLongitudeMaison());
        assertEquals(LocalDate.of(2026, 5, 4), demande.getDateSouhaitee());
        assertEquals(LocalTime.of(16, 30), demande.getHeureSouhaitee());
        assertTrue(demande.getSuspicious());
        assertTrue(demande.getAiAnalysisAvailable());
        assertTrue(demande.getDuplicateDetected());
        assertEquals(0.91, demande.getAnomalyScore());
        assertEquals("HIGH", demande.getAnomalyLevel());
        assertEquals("Adresse inhabituelle", demande.getAnomalyReasons());
        assertEquals("v-test", demande.getAiModelVersion());
        assertEquals("none", demande.getAiAnalysisError());
        assertEquals("Merci de confirmer", demande.getRevisionRequestMessage());
        assertEquals(LocalDateTime.of(2026, 4, 29, 10, 15), demande.getRevisionRequestedAt());
        assertEquals(100L, aiExistingRequestDto.childId());
        assertEquals(LocalDate.of(2026, 5, 4), aiExistingRequestDto.requestDate());
        assertEquals(16, aiExistingRequestDto.requestedHour());
        assertEquals("Rue 2", aiExistingRequestDto.pickupAddress());
        assertEquals("Garderie Centrale", aiExistingRequestDto.dropoffAddress());
        assertEquals(20L, suggestion.demandeId());
        assertEquals(30L, suggestion.enfantId());
        assertEquals("Sami Doe", suggestion.enfantNomComplet());
        assertEquals(SensTrajetDemandeTransport.GARDERIE_VERS_MAISON, suggestion.sensTrajet());
        assertEquals("Menzah", suggestion.zoneRecherchee());
        assertEquals("Garderie Centrale", suggestion.destinationSouhaitee());
    }

    private void setId(Object target, Long id) throws Exception {
        Field field = target.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(target, id);
    }
}
