package com.tinyspring.garderie.controller.transport;

import com.tinyspring.garderie.dto.transport.admin.AdminDemandPredictionResponse;
import com.tinyspring.garderie.dto.transport.admin.AffectationTransportResponse;
import com.tinyspring.garderie.dto.transport.admin.DemandeAffectationRecommendationResponse;
import com.tinyspring.garderie.dto.transport.admin.NouveauTrajetRecommendationResponse;
import com.tinyspring.garderie.dto.transport.admin.TrajetRequest;
import com.tinyspring.garderie.dto.transport.admin.TrajetResponse;
import com.tinyspring.garderie.dto.transport.admin.TransportRequest;
import com.tinyspring.garderie.dto.transport.admin.TransportResponse;
import com.tinyspring.garderie.entity.transport.SensTrajetDemandeTransport;
import com.tinyspring.garderie.entity.transport.StatutDemandeTransport;
import com.tinyspring.garderie.service.transport.admin.TransportAdminService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransportAdminControllerTest {

    @Mock
    private TransportAdminService transportAdminService;

    @InjectMocks
    private TransportAdminController transportAdminController;

    @Test
    void shouldListTransports() {
        TransportResponse expected = new TransportResponse(1L, "Bus A", "TN-001", 12, 25.0);
        when(transportAdminService.listerTransports()).thenReturn(List.of(expected));

        List<TransportResponse> response = transportAdminController.getTransports();

        assertEquals(List.of(expected), response);
    }

    @Test
    void shouldCreateTransport() {
        TransportRequest request = new TransportRequest();
        request.setNom("Bus B");
        request.setMatricule("TN-002");
        request.setCapacite(16);
        TransportResponse expected = new TransportResponse(2L, "Bus B", "TN-002", 16, 0.0);
        when(transportAdminService.creerTransport(request)).thenReturn(expected);

        TransportResponse response = transportAdminController.createTransport(request);

        assertEquals(expected, response);
    }

    @Test
    void shouldUpdateTransport() {
        TransportRequest request = new TransportRequest();
        request.setNom("Bus C");
        request.setMatricule("TN-003");
        request.setCapacite(18);
        TransportResponse expected = new TransportResponse(3L, "Bus C", "TN-003", 18, 33.0);
        when(transportAdminService.modifierTransport(3L, request)).thenReturn(expected);

        TransportResponse response = transportAdminController.updateTransport(3L, request);

        assertEquals(expected, response);
    }

    @Test
    void shouldDeleteTransport() {
        transportAdminController.deleteTransport(4L);

        verify(transportAdminService).supprimerTransport(4L);
    }

    @Test
    void shouldListTrajets() {
        TrajetResponse expected = new TrajetResponse(
                5L,
                "Ariana",
                "Garderie",
                "Zone Nord",
                36.8,
                10.1,
                LocalDate.of(2026, 5, 4),
                LocalTime.of(8, 0),
                1L,
                "Bus A",
                "TN-001"
        );
        when(transportAdminService.listerTrajets()).thenReturn(List.of(expected));

        List<TrajetResponse> response = transportAdminController.getTrajets();

        assertEquals(List.of(expected), response);
    }

    @Test
    void shouldCreateTrajet() {
        TrajetRequest request = new TrajetRequest();
        request.setPointDepart("Menzah");
        request.setDestination("Garderie");
        request.setDateTrajet(LocalDate.of(2026, 5, 5));
        request.setHeureDepart(LocalTime.of(8, 30));
        request.setTransportId(7L);
        TrajetResponse expected = new TrajetResponse(
                6L,
                "Menzah",
                "Garderie",
                null,
                null,
                null,
                LocalDate.of(2026, 5, 5),
                LocalTime.of(8, 30),
                7L,
                "Bus M",
                "TN-010"
        );
        when(transportAdminService.creerTrajet(request)).thenReturn(expected);

        TrajetResponse response = transportAdminController.createTrajet(request);

        assertEquals(expected, response);
    }

    @Test
    void shouldUpdateTrajet() {
        TrajetRequest request = new TrajetRequest();
        request.setPointDepart("Lac");
        request.setDestination("Garderie");
        request.setDateTrajet(LocalDate.of(2026, 5, 6));
        request.setHeureDepart(LocalTime.of(9, 0));
        request.setTransportId(8L);
        TrajetResponse expected = new TrajetResponse(
                7L,
                "Lac",
                "Garderie",
                "Zone Est",
                36.85,
                10.25,
                LocalDate.of(2026, 5, 6),
                LocalTime.of(9, 0),
                8L,
                "Bus E",
                "TN-011"
        );
        when(transportAdminService.modifierTrajet(7L, request)).thenReturn(expected);

        TrajetResponse response = transportAdminController.updateTrajet(7L, request);

        assertEquals(expected, response);
    }

    @Test
    void shouldDeleteTrajet() {
        transportAdminController.deleteTrajet(8L);

        verify(transportAdminService).supprimerTrajet(8L);
    }

    @Test
    void shouldListAffectations() {
        AffectationTransportResponse expected = new AffectationTransportResponse(
                9L,
                2L,
                "Adam Test",
                1L,
                "Bus A",
                "TN-001",
                6L,
                "Ariana",
                "Garderie",
                LocalDate.of(2026, 5, 4),
                LocalTime.of(8, 0),
                "Maison"
        );
        when(transportAdminService.listerAffectations()).thenReturn(List.of(expected));

        List<AffectationTransportResponse> response = transportAdminController.getAffectations();

        assertEquals(List.of(expected), response);
    }

    @Test
    void shouldListAffectationRecommendations() {
        DemandeAffectationRecommendationResponse expected = new DemandeAffectationRecommendationResponse(
                10L,
                2L,
                "Lina Test",
                StatutDemandeTransport.EN_ATTENTE,
                SensTrajetDemandeTransport.MAISON_VERS_GARDERIE,
                "Ariana",
                "Garderie",
                true,
                92,
                1.5,
                "AUTO",
                null,
                6L,
                "Ariana",
                "Garderie",
                LocalDate.of(2026, 5, 4),
                LocalTime.of(8, 0),
                1L,
                "Bus A",
                "Zone Nord"
        );
        when(transportAdminService.listerRecommandationsAffectation()).thenReturn(List.of(expected));

        List<DemandeAffectationRecommendationResponse> response = transportAdminController.getRecommandationsAffectation();

        assertEquals(List.of(expected), response);
    }

    @Test
    void shouldListNewRouteRecommendations() {
        NouveauTrajetRecommendationResponse expected = new NouveauTrajetRecommendationResponse(
                "Ariana Centre",
                36.86,
                10.17,
                4,
                2.8,
                "Creer un nouveau trajet",
                List.of()
        );
        when(transportAdminService.listerRecommandationsNouveauxTrajets()).thenReturn(List.of(expected));

        List<NouveauTrajetRecommendationResponse> response = transportAdminController.getRecommandationsNouveauxTrajets();

        assertEquals(List.of(expected), response);
    }

    @Test
    void shouldGetPrediction() {
        AdminDemandPredictionResponse expected = new AdminDemandPredictionResponse(
                LocalDate.of(2026, 5, 7),
                8,
                40L,
                5L,
                5.4,
                22,
                "ELEVEE",
                true,
                "model-v2",
                "Prediction generee"
        );
        when(transportAdminService.predireDemandeAdmin(LocalDate.of(2026, 5, 7), 8, true, false)).thenReturn(expected);

        AdminDemandPredictionResponse response = transportAdminController.getPredictionDemande(
                LocalDate.of(2026, 5, 7),
                8,
                true,
                false
        );

        assertEquals(expected, response);
    }
}
