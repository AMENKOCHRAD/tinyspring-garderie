package com.tinyspring.garderie.controller.events;

import com.tinyspring.garderie.dto.Events.DishRequest;
import com.tinyspring.garderie.dto.Events.DishResponse;
import com.tinyspring.garderie.service.events.DishService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DishRestControllerTest {

    @Mock
    private DishService dishService;

    @InjectMocks
    private DishRestController controller;

    @Test
    void create_shouldReturnCreatedDish() {
        DishRequest request = new DishRequest();
        DishResponse response = new DishResponse();

        when(dishService.create(request)).thenReturn(response);

        ResponseEntity<DishResponse> result = controller.create(request);

        assertEquals(201, result.getStatusCode().value());
        assertSame(response, result.getBody());

        verify(dishService).create(request);
    }

    @Test
    void update_shouldReturnUpdatedDish() {
        DishRequest request = new DishRequest();
        DishResponse response = new DishResponse();

        when(dishService.update(1L, request)).thenReturn(response);

        ResponseEntity<DishResponse> result = controller.update(1L, request);

        assertEquals(200, result.getStatusCode().value());
        assertSame(response, result.getBody());

        verify(dishService).update(1L, request);
    }

    @Test
    void delete_shouldReturnNoContent() {
        ResponseEntity<Void> result = controller.delete(1L);

        assertEquals(204, result.getStatusCode().value());
        assertNull(result.getBody());

        verify(dishService).delete(1L);
    }
}