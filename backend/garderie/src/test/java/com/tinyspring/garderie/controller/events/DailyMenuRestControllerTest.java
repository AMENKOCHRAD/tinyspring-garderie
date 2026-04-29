package com.tinyspring.garderie.controller.events;

import com.tinyspring.garderie.dto.Events.DailyMenuRequest;
import com.tinyspring.garderie.dto.Events.DailyMenuResponse;
import com.tinyspring.garderie.service.events.DailyMenuService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyMenuRestControllerTest {

    @Mock
    private DailyMenuService dailyMenuService;

    @InjectMocks
    private DailyMenuRestController controller;

    @Test
    void create_shouldReturnCreatedMenu() {
        DailyMenuRequest request = new DailyMenuRequest();
        DailyMenuResponse response = new DailyMenuResponse();

        when(dailyMenuService.create(request)).thenReturn(response);

        ResponseEntity<DailyMenuResponse> result = controller.create(request);

        assertEquals(201, result.getStatusCode().value());
        assertSame(response, result.getBody());

        verify(dailyMenuService).create(request);
    }

    @Test
    void update_shouldReturnUpdatedMenu() {
        DailyMenuRequest request = new DailyMenuRequest();
        DailyMenuResponse response = new DailyMenuResponse();

        when(dailyMenuService.update(1L, request)).thenReturn(response);

        ResponseEntity<DailyMenuResponse> result = controller.update(1L, request);

        assertEquals(200, result.getStatusCode().value());
        assertSame(response, result.getBody());

        verify(dailyMenuService).update(1L, request);
    }

    @Test
    void delete_shouldReturnNoContent() {
        ResponseEntity<Void> result = controller.delete(1L);

        assertEquals(204, result.getStatusCode().value());
        assertNull(result.getBody());

        verify(dailyMenuService).delete(1L);
    }
}