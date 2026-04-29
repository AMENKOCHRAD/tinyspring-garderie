package com.tinyspring.garderie.controller.transport;

import com.tinyspring.garderie.dto.transport.DemandeTransportResponse;
import com.tinyspring.garderie.dto.transport.EnfantTrajetResponse;
import com.tinyspring.garderie.dto.transport.TrajetDetailsResponse;
import com.tinyspring.garderie.dto.transport.TraitementDemandeTransportResponse;
import com.tinyspring.garderie.entity.transport.SensTrajetDemandeTransport;
import com.tinyspring.garderie.entity.transport.StatutDemandeTransport;
import com.tinyspring.garderie.service.transport.TransportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransportControllerTest {

    @Mock
    private TransportService transportService;

    @InjectMocks
    private TransportController transportController;

    @Test
    void shouldReturnAllDemandes() {
        DemandeTransportResponse demande = buildDemandeResponse(11L, StatutDemandeTransport.EN_ATTENTE);
        when(transportService.listerToutesLesDemandes()).thenReturn(List.of(demande));

        List<DemandeTransportResponse> responses = transportController.getDemandes();

        assertEquals(1, responses.size());
        assertEquals(11L, responses.get(0).id());
    }

    @Test
    void shouldAcceptDemande() {
        TraitementDemandeTransportResponse expected = new TraitementDemandeTransportResponse(
                11L,
                StatutDemandeTransport.ACCEPTEE,
                81L,
                40.0,
                "Demande acceptee."
        );
        when(transportService.accepterDemande(11L)).thenReturn(expected);

        TraitementDemandeTransportResponse response = transportController.accepterDemande(11L);

        assertEquals(expected, response);
    }

    @Test
    void shouldRefuseDemande() {
        TraitementDemandeTransportResponse expected = new TraitementDemandeTransportResponse(
                12L,
                StatutDemandeTransport.REFUSEE,
                null,
                null,
                "Demande refusee."
        );
        when(transportService.refuserDemande(12L)).thenReturn(expected);

        TraitementDemandeTransportResponse response = transportController.refuserDemande(12L);

        assertEquals(expected, response);
    }

    @Test
    void shouldDeleteDemande() {
        transportController.supprimerDemande(13L);

        verify(transportService).supprimerDemandeTransportAdmin(13L);
    }

    @Test
    void shouldReturnTrajetDetails() {
        TrajetDetailsResponse expected = new TrajetDetailsResponse(
                41L,
                "Ariana",
                "Garderie",
                LocalDate.of(2026, 5, 3),
                LocalTime.of(8, 0),
                1,
                List.of(new EnfantTrajetResponse(5L, "Lina Ben Salem", 91L, 7L, "Bus A", "TN-100", "Maison"))
        );
        when(transportService.listerEnfantsParTrajet(41L)).thenReturn(expected);

        TrajetDetailsResponse response = transportController.getEnfantsParTrajet(41L);

        assertEquals(expected, response);
    }

    private DemandeTransportResponse buildDemandeResponse(Long id, StatutDemandeTransport statut) {
        return new DemandeTransportResponse(
                id,
                2L,
                "Lina Test",
                3L,
                "Parent Test",
                4L,
                LocalDate.of(2026, 4, 20),
                "Maison",
                "Garderie",
                LocalDate.of(2026, 4, 21),
                LocalTime.of(8, 0),
                statut,
                "Maison",
                "Garderie",
                SensTrajetDemandeTransport.MAISON_VERS_GARDERIE,
                "Ariana",
                36.8,
                10.1,
                "Garderie",
                LocalDate.of(2026, 4, 21),
                LocalTime.of(7, 45),
                false,
                0.1,
                "LOW",
                List.of("RAS"),
                false,
                true,
                "model-v1",
                null,
                null,
                LocalDateTime.of(2026, 4, 20, 10, 0)
        );
    }
}
