package com.tinyspring.garderie.service.events;

import com.tinyspring.garderie.dto.Events.EventRequest;
import com.tinyspring.garderie.entity.events.Event;
import com.tinyspring.garderie.entity.events.EventStatus;
import com.tinyspring.garderie.exception.Events.InvalidStatusTransitionException;
import com.tinyspring.garderie.exception.Events.ResourceNotFoundException;
import com.tinyspring.garderie.mappeer.EventMapper;
import com.tinyspring.garderie.repository.Children.ChildRepository;
import com.tinyspring.garderie.repository.Classes.ClasseRepository;
import com.tinyspring.garderie.repository.events.EventRatingRepository;
import com.tinyspring.garderie.repository.events.EventRegistrationRepository;
import com.tinyspring.garderie.repository.events.EventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private ClasseRepository classeRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventRegistrationRepository eventRegistrationRepository;

    @Mock
    private EventMapper eventMapper;

    @Mock
    private EventRatingRepository eventRatingRepository;

    @Mock
    private ChildRepository childRepository;

    @Mock
    private ParentNotificationService parentNotificationService;

    @InjectMocks
    private EventServiceImpl eventService;

    @Test
    void create_shouldSaveEventWithDraftStatus_whenStatusIsNull() {
        EventRequest request = new EventRequest();
        request.setClassroomId(1L);
        request.setRequiresAuthorization(true);
        request.setEventPrice(20.0);

        Event event = new Event();
        event.setClassroomId(1L);
        event.setStartDatetime(LocalDateTime.now().plusDays(1));
        event.setEndDatetime(LocalDateTime.now().plusDays(2));

        when(classeRepository.existsById(1L)).thenReturn(true);
        when(eventMapper.toEntity(request)).thenReturn(event);
        when(eventRepository.save(event)).thenReturn(event);

        Event result = eventService.create(request);

        assertNotNull(result);
        assertEquals(EventStatus.DRAFT, result.getStatus());
        assertTrue(result.isRequiresAuthorization());
        assertEquals(20.0, result.getEventPrice());

        verify(eventRepository).save(event);
    }

    @Test
    void create_shouldThrowException_whenClassroomDoesNotExist() {
        EventRequest request = new EventRequest();
        request.setClassroomId(99L);

        when(classeRepository.existsById(99L)).thenReturn(false);

        assertThrows(InvalidStatusTransitionException.class,
                () -> eventService.create(request));

        verify(eventRepository, never()).save(any());
    }

    @Test
    void create_shouldThrowException_whenEndDateIsBeforeStartDate() {
        EventRequest request = new EventRequest();
        request.setClassroomId(1L);

        Event event = new Event();
        event.setClassroomId(1L);
        event.setStartDatetime(LocalDateTime.now().plusDays(2));
        event.setEndDatetime(LocalDateTime.now().plusDays(1));

        when(classeRepository.existsById(1L)).thenReturn(true);
        when(eventMapper.toEntity(request)).thenReturn(event);

        assertThrows(InvalidStatusTransitionException.class,
                () -> eventService.create(request));

        verify(eventRepository, never()).save(any());
    }

    @Test
    void getById_shouldReturnEvent_whenEventExists() {
        Event event = new Event();
        event.setId(1L);
        event.setStatus(EventStatus.DRAFT);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        Event result = eventService.getById(1L);

        assertEquals(1L, result.getId());
        assertEquals(EventStatus.DRAFT, result.getStatus());
    }

    @Test
    void getById_shouldThrowException_whenEventDoesNotExist() {
        when(eventRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> eventService.getById(1L));
    }

    @Test
    void publish_shouldPublishEventAndNotifyParents_whenEventIsValid() {
        Event event = new Event();
        event.setId(1L);
        event.setTitle("Sortie éducative");
        event.setClassroomId(1L);
        event.setStatus(EventStatus.DRAFT);
        event.setStartDatetime(LocalDateTime.now().plusDays(1));
        event.setEndDatetime(LocalDateTime.now().plusDays(2));

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(classeRepository.existsById(1L)).thenReturn(true);
        when(eventMapper.deserializeClassroomIds(event.getTargetClassroomIds()))
                .thenReturn(null);
        when(eventRepository.save(event)).thenReturn(event);

        Event result = eventService.publish(1L);

        assertEquals(EventStatus.PUBLISHED, result.getStatus());

        verify(eventRepository).save(event);
        verify(parentNotificationService).notifyEventPublished(event);
    }

    @Test
    void publish_shouldThrowException_whenEventIsCancelled() {
        Event event = new Event();
        event.setId(1L);
        event.setStatus(EventStatus.CANCELLED);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThrows(InvalidStatusTransitionException.class,
                () -> eventService.publish(1L));

        verify(parentNotificationService, never()).notifyEventPublished(any());
    }

    @Test
    void delete_shouldDeleteEvent_whenNoRegistrationExists() {
        Event event = new Event();
        event.setId(1L);
        event.setStatus(EventStatus.DRAFT);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRegistrationRepository.existsByEventId(1L)).thenReturn(false);

        eventService.delete(1L);

        verify(eventRepository).delete(event);
    }

    @Test
    void delete_shouldThrowException_whenRegistrationsExist() {
        Event event = new Event();
        event.setId(1L);
        event.setStatus(EventStatus.PUBLISHED);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRegistrationRepository.existsByEventId(1L)).thenReturn(true);

        assertThrows(InvalidStatusTransitionException.class,
                () -> eventService.delete(1L));

        verify(eventRepository, never()).delete(any());
    }

    @Test
    void getStatusPriority_shouldReturnCorrectPriority() {
        assertEquals(1, eventService.getStatusPriority(EventStatus.PUBLISHED));
        assertEquals(2, eventService.getStatusPriority(EventStatus.DRAFT));
        assertEquals(3, eventService.getStatusPriority(EventStatus.COMPLETED));
        assertEquals(4, eventService.getStatusPriority(EventStatus.CANCELLED));
        assertEquals(99, eventService.getStatusPriority(null));
    }
}