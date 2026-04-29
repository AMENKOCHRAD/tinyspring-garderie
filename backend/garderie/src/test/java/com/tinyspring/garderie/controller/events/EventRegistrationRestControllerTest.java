package com.tinyspring.garderie.controller.events;

import com.tinyspring.garderie.dto.Events.EventRegistrationRequest;
import com.tinyspring.garderie.dto.Events.EventRegistrationResponse;
import com.tinyspring.garderie.entity.events.EventRegistration;
import com.tinyspring.garderie.mappeer.EventRegistrationMapper;
import com.tinyspring.garderie.service.events.EventRegistrationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventRegistrationRestControllerTest {

    @Mock
    private EventRegistrationService eventRegistrationService;

    @Mock
    private EventRegistrationMapper eventRegistrationMapper;

    @InjectMocks
    private EventRegistrationRestController controller;

    @Test
    void register_shouldReturnCreatedRegistration() {
        EventRegistrationRequest request = new EventRegistrationRequest();
        EventRegistration registration = new EventRegistration();
        EventRegistrationResponse response = new EventRegistrationResponse();

        when(eventRegistrationService.register(1L, request)).thenReturn(registration);
        when(eventRegistrationMapper.toResponse(registration)).thenReturn(response);

        ResponseEntity<EventRegistrationResponse> result = controller.register(1L, request);

        assertEquals(201, result.getStatusCode().value());
        assertSame(response, result.getBody());

        verify(eventRegistrationService).register(1L, request);
        verify(eventRegistrationMapper).toResponse(registration);
    }

    @Test
    void getByEventId_shouldReturnRegistrations() {
        EventRegistration registration = new EventRegistration();
        EventRegistrationResponse response = new EventRegistrationResponse();

        when(eventRegistrationService.getByEventId(1L)).thenReturn(List.of(registration));
        when(eventRegistrationMapper.toResponse(registration)).thenReturn(response);

        ResponseEntity<List<EventRegistrationResponse>> result = controller.getByEventId(1L);

        assertEquals(200, result.getStatusCode().value());
        assertEquals(1, result.getBody().size());
        assertSame(response, result.getBody().get(0));

        verify(eventRegistrationService).getByEventId(1L);
    }

    @Test
    void confirm_shouldReturnConfirmedRegistration() {
        EventRegistration registration = new EventRegistration();
        EventRegistrationResponse response = new EventRegistrationResponse();

        when(eventRegistrationService.confirm(1L)).thenReturn(registration);
        when(eventRegistrationMapper.toResponse(registration)).thenReturn(response);

        ResponseEntity<EventRegistrationResponse> result = controller.confirm(1L);

        assertEquals(200, result.getStatusCode().value());
        assertSame(response, result.getBody());

        verify(eventRegistrationService).confirm(1L);
    }

    @Test
    void cancel_shouldReturnCancelledRegistration() {
        EventRegistration registration = new EventRegistration();
        EventRegistrationResponse response = new EventRegistrationResponse();

        when(eventRegistrationService.cancel(1L)).thenReturn(registration);
        when(eventRegistrationMapper.toResponse(registration)).thenReturn(response);

        ResponseEntity<EventRegistrationResponse> result = controller.cancel(1L);

        assertEquals(200, result.getStatusCode().value());
        assertSame(response, result.getBody());

        verify(eventRegistrationService).cancel(1L);
    }

    @Test
    void markAttended_shouldReturnAttendedRegistration() {
        EventRegistration registration = new EventRegistration();
        EventRegistrationResponse response = new EventRegistrationResponse();

        when(eventRegistrationService.markAttended(1L)).thenReturn(registration);
        when(eventRegistrationMapper.toResponse(registration)).thenReturn(response);

        ResponseEntity<EventRegistrationResponse> result = controller.markAttended(1L);

        assertEquals(200, result.getStatusCode().value());
        assertSame(response, result.getBody());

        verify(eventRegistrationService).markAttended(1L);
    }

    @Test
    void markAbsent_shouldReturnAbsentRegistration() {
        EventRegistration registration = new EventRegistration();
        EventRegistrationResponse response = new EventRegistrationResponse();

        when(eventRegistrationService.markAbsent(1L)).thenReturn(registration);
        when(eventRegistrationMapper.toResponse(registration)).thenReturn(response);

        ResponseEntity<EventRegistrationResponse> result = controller.markAbsent(1L);

        assertEquals(200, result.getStatusCode().value());
        assertSame(response, result.getBody());

        verify(eventRegistrationService).markAbsent(1L);
    }
}