package com.tinyspring.garderie.service.transport.ai.impl;

import com.tinyspring.garderie.config.transport.TransportAiProperties;
import com.tinyspring.garderie.dto.transport.admin.AdminDemandPredictionResponse;
import com.tinyspring.garderie.entity.Role;
import com.tinyspring.garderie.entity.RoleName;
import com.tinyspring.garderie.entity.User;
import com.tinyspring.garderie.entity.transport.DemandeTransport;
import com.tinyspring.garderie.entity.transport.Enfant;
import com.tinyspring.garderie.entity.transport.SensTrajetDemandeTransport;
import com.tinyspring.garderie.entity.transport.StatutDemandeTransport;
import com.tinyspring.garderie.entity.transport.Trajet;
import com.tinyspring.garderie.entity.transport.Transport;
import com.tinyspring.garderie.repository.transport.DemandeTransportRepository;
import com.tinyspring.garderie.repository.transport.EnfantRepository;
import com.tinyspring.garderie.repository.transport.TrajetRepository;
import com.tinyspring.garderie.repository.transport.TransportRepository;
import com.tinyspring.garderie.service.transport.ai.AiAnomalyAnalysisResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransportAiServiceImplTest {

    @Mock
    private DemandeTransportRepository demandeTransportRepository;

    @Mock
    private EnfantRepository enfantRepository;

    @Mock
    private TransportRepository transportRepository;

    @Mock
    private TrajetRepository trajetRepository;

    @Test
    void shouldReturnSuccessfulAiAnalysisWhenMicroserviceResponds() {
        TransportAiServiceImpl service = buildService(jsonClient("""
                {
                  "is_anomaly": true,
                  "anomaly_score": 0.87,
                  "anomaly_level": "HIGH",
                  "reasons": ["Adresse inhabituelle"],
                  "duplicate_found": true,
                  "model_version": "v-test"
                }
                """));
        DemandeTransport demande = buildDemande(1L, 101L, "Les Berges du Lac", 36.84, 10.27);

        when(demandeTransportRepository.findTop10ByEnfantIdOrderByIdDesc(101L)).thenReturn(List.of());

        AiAnomalyAnalysisResult result = service.analyserDemande(demande, null);

        assertTrue(result.aiAvailable());
        assertTrue(result.suspicious());
        assertEquals(0.87, result.anomalyScore());
        assertEquals("HIGH", result.anomalyLevel());
        assertEquals(List.of("Adresse inhabituelle"), result.anomalyReasons());
        assertTrue(result.duplicateFound());
        assertEquals("v-test", result.modelVersion());
    }

    @Test
    void shouldReturnUnavailableAnalysisWhenMicroserviceFails() {
        TransportAiServiceImpl service = buildService(failingClient());
        DemandeTransport demande = buildDemande(2L, 102L, "Borj Touil", 36.92, 10.19);

        when(demandeTransportRepository.findTop10ByEnfantIdOrderByIdDesc(102L)).thenReturn(List.of());

        AiAnomalyAnalysisResult result = service.analyserDemande(demande, null);

        assertFalse(result.aiAvailable());
        assertFalse(result.suspicious());
        assertEquals("UNKNOWN", result.anomalyLevel());
        assertNotNull(result.errorMessage());
        assertTrue(result.errorMessage().contains("Microservice IA indisponible"));
    }

    @Test
    void shouldReturnPredictionFromMicroservice() {
        TransportAiServiceImpl service = buildService(jsonClient("""
                {
                  "predicted_demand_count": 14,
                  "demand_level": "HIGH",
                  "model_version": "v-predict"
                }
                """));

        when(enfantRepository.count()).thenReturn(12L);
        when(transportRepository.count()).thenReturn(3L);
        when(trajetRepository.findAll()).thenReturn(List.of(buildTrajet(1L, 36.84, 10.27)));

        AdminDemandPredictionResponse response = service.predireDemandePourDashboard(LocalDate.of(2026, 4, 30), 8, true, false);

        assertTrue(response.aiAvailable());
        assertEquals(14, response.predictedDemandCount());
        assertEquals("HIGH", response.demandLevel());
        assertEquals("v-predict", response.modelVersion());
    }

    @Test
    void shouldFallbackPredictionWhenMicroserviceIsUnavailable() {
        TransportAiServiceImpl service = buildService(failingClient());

        when(enfantRepository.count()).thenReturn(8L);
        when(transportRepository.count()).thenReturn(2L);
        when(trajetRepository.findAll()).thenReturn(List.of());

        AdminDemandPredictionResponse response = service.predireDemandePourDashboard(null, null, false, false);

        assertFalse(response.aiAvailable());
        assertEquals("UNKNOWN", response.demandLevel());
        assertEquals(null, response.predictedDemandCount());
        assertTrue(response.message().contains("Microservice IA indisponible"));
    }

    private TransportAiServiceImpl buildService(WebClient client) {
        TransportAiProperties properties = new TransportAiProperties();
        properties.setTimeoutSeconds(2);
        return new TransportAiServiceImpl(
                client,
                properties,
                demandeTransportRepository,
                enfantRepository,
                transportRepository,
                trajetRepository
        );
    }

    private WebClient jsonClient(String jsonBody) {
        ExchangeFunction exchangeFunction = request -> Mono.just(
                ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(jsonBody)
                        .build()
        );
        return WebClient.builder().exchangeFunction(exchangeFunction).build();
    }

    private WebClient failingClient() {
        ExchangeFunction exchangeFunction = request -> Mono.error(new RuntimeException("connection refused"));
        return WebClient.builder().exchangeFunction(exchangeFunction).build();
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
                "15 Rue des Ecoles, El Menzah 5, Ariana 2091, Tunisie",
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

    private Trajet buildTrajet(Long id, double latitude, double longitude) {
        Transport transport = new Transport("Bus " + id, "TR-" + id, 12);
        setId(transport, 500L + id);

        Trajet trajet = new Trajet("Lac 1", "15 Rue des Ecoles, El Menzah 5, Ariana 2091, Tunisie",
                LocalDate.now().plusDays(1), LocalTime.of(7, 30), transport);
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
