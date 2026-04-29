package com.tinyspring.garderie.controller.events;

import com.tinyspring.garderie.dto.Events.*;
import com.tinyspring.garderie.entity.events.Event;
import com.tinyspring.garderie.entity.events.EventRating;
import com.tinyspring.garderie.entity.events.RegistrationStatus;
import com.tinyspring.garderie.mappeer.EventMapper;
import com.tinyspring.garderie.repository.events.EventRatingRepository;
import com.tinyspring.garderie.repository.events.EventRegistrationRepository;
import com.tinyspring.garderie.service.events.EventRecommendationService;
import com.tinyspring.garderie.service.events.EventService;
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
class EventRestControllerTest {

    @Mock private EventService eventService;
    @Mock private EventMapper eventMapper;
    @Mock private EventRegistrationRepository eventRegistrationRepository;
    @Mock private EventRecommendationService eventRecommendationService;
    @Mock private EventRatingRepository eventRatingRepository;

    @InjectMocks
    private EventRestController controller;

    @Test
    void create_shouldReturnCreatedEvent() {
        EventRequest request = new EventRequest();
        Event event = new Event();
        event.setId(1L);

        EventResponse response = new EventResponse();

        when(eventService.create(request)).thenReturn(event);
        when(eventMapper.toResponse(event)).thenReturn(response);
        when(eventRegistrationRepository.countByEventIdAndStatusIn(anyLong(), anySet())).thenReturn(0L);
        when(eventRegistrationRepository.countByEventIdAndStatus(anyLong(), any())).thenReturn(0L);
        when(eventRatingRepository.findByEventId(anyLong())).thenReturn(List.of());

        ResponseEntity<EventResponse> result = controller.create(request);

        assertEquals(201, result.getStatusCode().value());
        assertSame(response, result.getBody());
    }

    @Test
    void getAll_shouldReturnAllEvents() {
        when(eventService.getAllWithRatings()).thenReturn(List.of(new EventResponse()));

        ResponseEntity<List<EventResponse>> result = controller.getAll();

        assertEquals(200, result.getStatusCode().value());
        assertEquals(1, result.getBody().size());
    }

    @Test
    void getPublished_shouldReturnPublishedEvents() {
        Event event = new Event();
        event.setId(1L);

        EventResponse response = new EventResponse();

        when(eventService.getPublished()).thenReturn(List.of(event));
        when(eventMapper.toResponse(event)).thenReturn(response);
        when(eventRegistrationRepository.countByEventIdAndStatusIn(anyLong(), anySet())).thenReturn(0L);
        when(eventRegistrationRepository.countByEventIdAndStatus(anyLong(), any())).thenReturn(0L);
        when(eventRatingRepository.findByEventId(anyLong())).thenReturn(List.of());

        ResponseEntity<List<EventResponse>> result = controller.getPublished();

        assertEquals(200, result.getStatusCode().value());
        assertEquals(1, result.getBody().size());
    }

    @Test
    void getById_shouldReturnEvent() {
        Event event = new Event();
        event.setId(1L);

        EventResponse response = new EventResponse();

        when(eventService.getById(1L)).thenReturn(event);
        when(eventMapper.toResponse(event)).thenReturn(response);
        when(eventRegistrationRepository.countByEventIdAndStatusIn(anyLong(), anySet())).thenReturn(0L);
        when(eventRegistrationRepository.countByEventIdAndStatus(anyLong(), any())).thenReturn(0L);
        when(eventRatingRepository.findByEventId(anyLong())).thenReturn(List.of());

        ResponseEntity<EventResponse> result = controller.getById(1L);

        assertEquals(200, result.getStatusCode().value());
        assertSame(response, result.getBody());
    }

    @Test
    void delete_shouldReturnNoContent() {
        ResponseEntity<Void> result = controller.delete(1L);

        assertEquals(204, result.getStatusCode().value());
        verify(eventService).delete(1L);
    }

    @Test
    void publish_shouldReturnPublishedEvent() {
        Event event = new Event();
        event.setId(1L);

        EventResponse response = new EventResponse();

        when(eventService.publish(1L)).thenReturn(event);
        when(eventMapper.toResponse(event)).thenReturn(response);
        when(eventRegistrationRepository.countByEventIdAndStatusIn(anyLong(), anySet())).thenReturn(0L);
        when(eventRegistrationRepository.countByEventIdAndStatus(anyLong(), any())).thenReturn(0L);
        when(eventRatingRepository.findByEventId(anyLong())).thenReturn(List.of());

        ResponseEntity<EventResponse> result = controller.publish(1L);

        assertEquals(200, result.getStatusCode().value());
        assertSame(response, result.getBody());
    }

    @Test
    void recommendEvents_shouldReturnRecommendations() {
        EventRecommendationContextRequest request = new EventRecommendationContextRequest();

        when(eventRecommendationService.recommendEvents(request))
                .thenReturn(List.of());

        ResponseEntity<?> result = controller.recommendEvents(request);

        assertEquals(200, result.getStatusCode().value());
    }

    @Test
    void recommendEvents_shouldReturnError_whenExceptionOccurs() {
        EventRecommendationContextRequest request = new EventRecommendationContextRequest();

        when(eventRecommendationService.recommendEvents(request))
                .thenThrow(new RuntimeException("AI error"));

        ResponseEntity<?> result = controller.recommendEvents(request);

        assertEquals(500, result.getStatusCode().value());
    }
}