package com.tinyspring.garderie.controller.events;

import com.tinyspring.garderie.dto.Events.WeeklyMenuAiGenerateRequest;
import com.tinyspring.garderie.dto.Events.WeeklyMenuRequest;
import com.tinyspring.garderie.dto.Events.WeeklyMenuResponse;
import com.tinyspring.garderie.service.events.MenuAiService;
import com.tinyspring.garderie.service.events.WeeklyMenuService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeeklyMenuRestControllerTest {

    @Mock
    private WeeklyMenuService weeklyMenuService;

    @Mock
    private MenuAiService menuAiService;

    @InjectMocks
    private WeeklyMenuRestController controller;

    @Test
    void getAll_shouldReturnAllMenus() {
        WeeklyMenuResponse response = new WeeklyMenuResponse();

        when(weeklyMenuService.getAll()).thenReturn(List.of(response));

        ResponseEntity<List<WeeklyMenuResponse>> result = controller.getAll();

        assertEquals(200, result.getStatusCode().value());
        assertEquals(1, result.getBody().size());
        verify(weeklyMenuService).getAll();
    }

    @Test
    void create_shouldReturnCreatedMenu() {
        WeeklyMenuRequest request = new WeeklyMenuRequest();
        WeeklyMenuResponse response = new WeeklyMenuResponse();

        when(weeklyMenuService.create(request)).thenReturn(response);

        ResponseEntity<WeeklyMenuResponse> result = controller.create(request);

        assertEquals(201, result.getStatusCode().value());
        assertSame(response, result.getBody());
        verify(weeklyMenuService).create(request);
    }

    @Test
    void getById_shouldReturnMenu() {
        WeeklyMenuResponse response = new WeeklyMenuResponse();

        when(weeklyMenuService.getById(1L)).thenReturn(response);

        ResponseEntity<WeeklyMenuResponse> result = controller.getById(1L);

        assertEquals(200, result.getStatusCode().value());
        assertSame(response, result.getBody());
        verify(weeklyMenuService).getById(1L);
    }

    @Test
    void update_shouldReturnUpdatedMenu() {
        WeeklyMenuRequest request = new WeeklyMenuRequest();
        WeeklyMenuResponse response = new WeeklyMenuResponse();

        when(weeklyMenuService.update(1L, request)).thenReturn(response);

        ResponseEntity<WeeklyMenuResponse> result = controller.update(1L, request);

        assertEquals(200, result.getStatusCode().value());
        assertSame(response, result.getBody());
        verify(weeklyMenuService).update(1L, request);
    }

    @Test
    void delete_shouldReturnNoContent() {
        ResponseEntity<Void> result = controller.delete(1L);

        assertEquals(204, result.getStatusCode().value());
        assertNull(result.getBody());
        verify(weeklyMenuService).delete(1L);
    }

    @Test
    void duplicate_shouldReturnCreatedMenu() {
        WeeklyMenuResponse response = new WeeklyMenuResponse();

        when(weeklyMenuService.duplicate(1L)).thenReturn(response);

        ResponseEntity<WeeklyMenuResponse> result = controller.duplicate(1L);

        assertEquals(201, result.getStatusCode().value());
        assertSame(response, result.getBody());
        verify(weeklyMenuService).duplicate(1L);
    }

    @Test
    void pingAi_shouldReturnReachableMessage() {
        ResponseEntity<String> result = controller.pingAi();

        assertEquals(200, result.getStatusCode().value());
        assertEquals("AI endpoint is reachable", result.getBody());
    }

    @Test
    void postTest_shouldReturnPostWorks() {
        ResponseEntity<String> result = controller.postTest();

        assertEquals(200, result.getStatusCode().value());
        assertEquals("POST works", result.getBody());
    }

    @Test
    void echo_shouldReturnSameBody() {
        ResponseEntity<String> result = controller.echo("{\"test\":true}");

        assertEquals(200, result.getStatusCode().value());
        assertEquals("{\"test\":true}", result.getBody());
    }

    @Test
    void generateTest_shouldReturnBodyOk() {
        ResponseEntity<String> result = controller.generateTest("{\"weekStartDate\":\"2026-04-27\"}");

        assertEquals(200, result.getStatusCode().value());
        assertEquals("BODY OK", result.getBody());
    }

    @Test
    void generateWithAi_shouldReturnBadRequest_whenWeekStartDateMissing() {
        ResponseEntity<?> result = controller.generateWithAi(Map.of());

        assertEquals(400, result.getStatusCode().value());

        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertEquals("weekStartDate est obligatoire", body.get("message"));

        verify(menuAiService, never()).generateWeeklyMenuDraft(any());
    }

    @Test
    void generateWithAi_shouldReturnGeneratedMenu_whenRequestIsValid() {
        WeeklyMenuRequest generatedMenu = new WeeklyMenuRequest();

        when(menuAiService.generateWeeklyMenuDraft(any(WeeklyMenuAiGenerateRequest.class)))
                .thenReturn(generatedMenu);

        ResponseEntity<?> result = controller.generateWithAi(Map.of(
                "weekStartDate", "2026-04-27"
        ));

        assertEquals(200, result.getStatusCode().value());
        assertSame(generatedMenu, result.getBody());

        verify(menuAiService).generateWeeklyMenuDraft(argThat(request ->
                "2026-04-27".equals(request.getWeekStartDate())
        ));
    }

    @Test
    void generateWithAi_shouldReturnInternalServerError_whenAiServiceFails() {
        when(menuAiService.generateWeeklyMenuDraft(any(WeeklyMenuAiGenerateRequest.class)))
                .thenThrow(new RuntimeException("AI down"));

        ResponseEntity<?> result = controller.generateWithAi(Map.of(
                "weekStartDate", "2026-04-27"
        ));

        assertEquals(500, result.getStatusCode().value());

        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertEquals("Erreur génération IA", body.get("message"));
        assertEquals("AI down", body.get("error"));
    }
}