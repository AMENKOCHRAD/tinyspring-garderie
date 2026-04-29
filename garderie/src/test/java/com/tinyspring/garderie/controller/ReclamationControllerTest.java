package com.tinyspring.garderie.controller;

import com.tinyspring.garderie.dto.AdminDashboardResponse;
import com.tinyspring.garderie.dto.EscalationInfoResponse;
import com.tinyspring.garderie.dto.RecommendedAdminActionResponse;
import com.tinyspring.garderie.dto.UpdateReclamationRequest;
import com.tinyspring.garderie.dto.UpdateReclamationStatusRequest;
import com.tinyspring.garderie.entity.Reclamation;
import com.tinyspring.garderie.entity.ReclamationHistory;
import com.tinyspring.garderie.service.ReclamationService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReclamationControllerTest {

    private final ReclamationService service = mock(ReclamationService.class);
    private final ReclamationController controller = new ReclamationController(service);

    @Test
    void createReclamationDelegatesToService() {
        Reclamation saved = new Reclamation();
        when(service.createReclamation("titre", "description", "HIGH", "SECURITE", null, null))
                .thenReturn(saved);

        Reclamation result = controller.createReclamation(
                "titre",
                "description",
                "HIGH",
                "SECURITE",
                null,
                null
        );

        assertSame(saved, result);
    }

    @Test
    void getAdminDashboardReturnsDashboardResponse() {
        AdminDashboardResponse dashboard = AdminDashboardResponse.builder()
                .totalActive(3)
                .build();
        when(service.getAdminDashboard()).thenReturn(dashboard);

        ResponseEntity<AdminDashboardResponse> response = controller.getAdminDashboard();

        assertEquals(3, response.getBody().getTotalActive());
    }

    @Test
    void standardReadEndpointsDelegateToService() {
        Reclamation reclamation = new Reclamation();
        ReclamationHistory history = new ReclamationHistory();
        when(service.getMyReclamations()).thenReturn(List.of(reclamation));
        when(service.getReclamationById(5L)).thenReturn(reclamation);
        when(service.getReclamationHistory(5L)).thenReturn(List.of(history));

        assertEquals(1, controller.getMyReclamations().size());
        assertSame(reclamation, controller.getReclamation(5L));
        assertEquals(1, controller.getReclamationHistory(5L).size());
    }

    @Test
    void exportEndpointsReturnBytes() {
        when(service.exportReclamationHistoryPdf(7L)).thenReturn(new byte[]{1, 2});
        when(service.exportReclamationsExcel()).thenReturn(new byte[]{3, 4});

        assertArrayEquals(new byte[]{1, 2}, controller.exportReclamationHistoryPdf(7L).getBody());
        assertArrayEquals(new byte[]{3, 4}, controller.exportReclamationsExcel().getBody());
    }

    @Test
    void assistantEndpointsReturnExpectedPayloads() {
        RecommendedAdminActionResponse action = new RecommendedAdminActionResponse(
                "SERVICE_TRANSPORT",
                "HIGH",
                "Verifier",
                "24H"
        );
        EscalationInfoResponse escalation = EscalationInfoResponse.builder()
                .reclamationId(4L)
                .autoEscalated(true)
                .build();

        when(service.generateSuggestedAdminResponse(4L)).thenReturn("Reponse suggeree");
        when(service.getRecommendedAdminAction(4L)).thenReturn(action);
        when(service.getEscalationInfo(4L)).thenReturn(escalation);

        ResponseEntity<Map<String, String>> suggestion = controller.getSuggestedResponse(4L);

        assertEquals("Reponse suggeree", suggestion.getBody().get("suggestedResponse"));
        assertEquals("SERVICE_TRANSPORT", controller.getRecommendedAdminAction(4L).getBody().getRecommendedService());
        assertEquals(4L, controller.getEscalationInfo(4L).getBody().getReclamationId());
    }

    @Test
    void updateAndDeleteEndpointsDelegateToService() {
        UpdateReclamationRequest updateRequest = new UpdateReclamationRequest();
        UpdateReclamationStatusRequest statusRequest = new UpdateReclamationStatusRequest("RESOLVED");
        Reclamation updated = new Reclamation();
        when(service.updateReclamation(9L, updateRequest)).thenReturn(updated);
        when(service.updateStatus(9L, statusRequest)).thenReturn(updated);

        assertSame(updated, controller.updateReclamation(9L, updateRequest));
        assertSame(updated, controller.updateStatus(9L, statusRequest));

        controller.deleteReclamation(9L);
        verify(service).deleteReclamation(9L);
    }
}
