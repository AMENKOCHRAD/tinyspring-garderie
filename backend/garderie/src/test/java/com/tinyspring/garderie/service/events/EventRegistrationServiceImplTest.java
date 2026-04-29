package com.tinyspring.garderie.service.events;

import com.tinyspring.garderie.dto.Events.EventRegistrationRequest;
import com.tinyspring.garderie.entity.events.Event;
import com.tinyspring.garderie.entity.events.EventRegistration;
import com.tinyspring.garderie.entity.events.EventStatus;
import com.tinyspring.garderie.entity.events.RegistrationStatus;
import com.tinyspring.garderie.exception.Events.AuthorizationRequiredException;
import com.tinyspring.garderie.exception.Events.InvalidStatusTransitionException;
import com.tinyspring.garderie.exception.Events.ResourceNotFoundException;
import com.tinyspring.garderie.repository.events.EventRegistrationRepository;
import com.tinyspring.garderie.repository.events.EventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventRegistrationServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventRegistrationRepository eventRegistrationRepository;

    @InjectMocks
    private EventRegistrationServiceImpl service;

    @Test
    void register_shouldCreateConfirmedRegistration_whenEventIsPublishedAndCapacityAvailable() {
        Event event = new Event();
        event.setId(1L);
        event.setStatus(EventStatus.PUBLISHED);
        event.setMaxCapacity(5);

        EventRegistrationRequest request = new EventRegistrationRequest();
        request.setChildId(10L);
        request.setParentId(20L);
        request.setAuthorizationSigned(true);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRegistrationRepository.existsByEventIdAndChildIdAndStatusIn(eq(1L), eq(10L), anySet()))
                .thenReturn(false);
        when(eventRegistrationRepository.countByEventIdAndStatusIn(eq(1L), anySet()))
                .thenReturn(2L);
        when(eventRegistrationRepository.save(any(EventRegistration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EventRegistration result = service.register(1L, request);

        assertEquals(RegistrationStatus.CONFIRMED, result.getStatus());
        assertEquals(10L, result.getChildId());
        assertEquals(20L, result.getParentId());
        assertTrue(result.isAuthorizationSigned());
    }

    @Test
    void register_shouldCreateWaitlistedRegistration_whenCapacityIsFull() {
        Event event = new Event();
        event.setId(1L);
        event.setStatus(EventStatus.PUBLISHED);
        event.setMaxCapacity(1);

        EventRegistrationRequest request = new EventRegistrationRequest();
        request.setChildId(10L);
        request.setParentId(20L);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRegistrationRepository.existsByEventIdAndChildIdAndStatusIn(eq(1L), eq(10L), anySet()))
                .thenReturn(false);
        when(eventRegistrationRepository.countByEventIdAndStatusIn(eq(1L), anySet()))
                .thenReturn(1L);
        when(eventRegistrationRepository.save(any(EventRegistration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EventRegistration result = service.register(1L, request);

        assertEquals(RegistrationStatus.WAITLISTED, result.getStatus());
    }

    @Test
    void register_shouldThrow_whenEventIsNotPublished() {
        Event event = new Event();
        event.setId(1L);
        event.setStatus(EventStatus.DRAFT);

        EventRegistrationRequest request = new EventRegistrationRequest();
        request.setChildId(10L);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThrows(InvalidStatusTransitionException.class,
                () -> service.register(1L, request));

        verify(eventRegistrationRepository, never()).save(any());
    }

    @Test
    void register_shouldThrow_whenAuthorizationRequiredButNotSigned() {
        Event event = new Event();
        event.setId(1L);
        event.setStatus(EventStatus.PUBLISHED);
        event.setRequiresAuthorization(true);

        EventRegistrationRequest request = new EventRegistrationRequest();
        request.setChildId(10L);
        request.setAuthorizationSigned(false);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRegistrationRepository.existsByEventIdAndChildIdAndStatusIn(eq(1L), eq(10L), anySet()))
                .thenReturn(false);

        assertThrows(AuthorizationRequiredException.class,
                () -> service.register(1L, request));
    }

    @Test
    void register_shouldThrow_whenChildAlreadyHasActiveRegistration() {
        Event event = new Event();
        event.setId(1L);
        event.setStatus(EventStatus.PUBLISHED);

        EventRegistrationRequest request = new EventRegistrationRequest();
        request.setChildId(10L);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRegistrationRepository.existsByEventIdAndChildIdAndStatusIn(eq(1L), eq(10L), anySet()))
                .thenReturn(true);

        assertThrows(InvalidStatusTransitionException.class,
                () -> service.register(1L, request));
    }

    @Test
    void getByEventId_shouldReturnRegistrations_whenEventExists() {
        Event event = new Event();
        event.setId(1L);

        EventRegistration registration = new EventRegistration();
        registration.setId(100L);
        registration.setEventId(1L);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRegistrationRepository.findByEventIdOrderByRegisteredAtAsc(1L))
                .thenReturn(List.of(registration));

        List<EventRegistration> result = service.getByEventId(1L);

        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).getId());
    }

    @Test
    void getByEventId_shouldThrow_whenEventNotFound() {
        when(eventRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getByEventId(1L));
    }

    @Test
    void confirm_shouldConfirmPendingRegistration_whenCapacityAvailable() {
        Event event = new Event();
        event.setId(1L);
        event.setMaxCapacity(3);
        event.setRequiresAuthorization(false);

        EventRegistration registration = new EventRegistration();
        registration.setId(100L);
        registration.setEventId(1L);
        registration.setStatus(RegistrationStatus.PENDING);

        when(eventRegistrationRepository.findById(100L)).thenReturn(Optional.of(registration));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRegistrationRepository.countByEventIdAndStatus(1L, RegistrationStatus.CONFIRMED))
                .thenReturn(1L);
        when(eventRegistrationRepository.save(registration)).thenReturn(registration);

        EventRegistration result = service.confirm(100L);

        assertEquals(RegistrationStatus.CONFIRMED, result.getStatus());
    }

    @Test
    void confirm_shouldMoveToWaitlist_whenCapacityFull() {
        Event event = new Event();
        event.setId(1L);
        event.setMaxCapacity(1);

        EventRegistration registration = new EventRegistration();
        registration.setId(100L);
        registration.setEventId(1L);
        registration.setStatus(RegistrationStatus.PENDING);

        when(eventRegistrationRepository.findById(100L)).thenReturn(Optional.of(registration));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRegistrationRepository.countByEventIdAndStatus(1L, RegistrationStatus.CONFIRMED))
                .thenReturn(1L);
        when(eventRegistrationRepository.save(registration)).thenReturn(registration);

        EventRegistration result = service.confirm(100L);

        assertEquals(RegistrationStatus.WAITLISTED, result.getStatus());
    }

    @Test
    void confirm_shouldThrow_whenRegistrationCancelled() {
        Event event = new Event();
        event.setId(1L);

        EventRegistration registration = new EventRegistration();
        registration.setId(100L);
        registration.setEventId(1L);
        registration.setStatus(RegistrationStatus.CANCELLED);

        when(eventRegistrationRepository.findById(100L)).thenReturn(Optional.of(registration));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThrows(InvalidStatusTransitionException.class,
                () -> service.confirm(100L));
    }

    @Test
    void cancel_shouldCancelRegistrationAndPromoteWaitlistedWhenSigned() {
        EventRegistration registration = new EventRegistration();
        registration.setId(100L);
        registration.setEventId(1L);
        registration.setStatus(RegistrationStatus.CONFIRMED);

        EventRegistration waitlisted = new EventRegistration();
        waitlisted.setId(101L);
        waitlisted.setEventId(1L);
        waitlisted.setStatus(RegistrationStatus.WAITLISTED);
        waitlisted.setAuthorizationSigned(true);

        when(eventRegistrationRepository.findById(100L)).thenReturn(Optional.of(registration));
        when(eventRegistrationRepository.save(any(EventRegistration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(eventRegistrationRepository.findFirstByEventIdAndStatusOrderByRegisteredAtAsc(
                1L, RegistrationStatus.WAITLISTED))
                .thenReturn(Optional.of(waitlisted));

        EventRegistration result = service.cancel(100L);

        assertEquals(RegistrationStatus.CANCELLED, result.getStatus());
        assertEquals(RegistrationStatus.CONFIRMED, waitlisted.getStatus());
        verify(eventRegistrationRepository, times(2)).save(any(EventRegistration.class));
    }

    @Test
    void markAttended_shouldMarkRegistrationAttended_whenEventCompleted() {
        Event event = new Event();
        event.setId(1L);
        event.setStatus(EventStatus.COMPLETED);

        EventRegistration registration = new EventRegistration();
        registration.setId(100L);
        registration.setEventId(1L);
        registration.setStatus(RegistrationStatus.CONFIRMED);

        when(eventRegistrationRepository.findById(100L)).thenReturn(Optional.of(registration));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRegistrationRepository.save(registration)).thenReturn(registration);

        EventRegistration result = service.markAttended(100L);

        assertEquals(RegistrationStatus.ATTENDED, result.getStatus());
    }

    @Test
    void markAbsent_shouldThrow_whenEventNotCompleted() {
        Event event = new Event();
        event.setId(1L);
        event.setStatus(EventStatus.PUBLISHED);

        EventRegistration registration = new EventRegistration();
        registration.setId(100L);
        registration.setEventId(1L);
        registration.setStatus(RegistrationStatus.CONFIRMED);

        when(eventRegistrationRepository.findById(100L)).thenReturn(Optional.of(registration));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThrows(InvalidStatusTransitionException.class,
                () -> service.markAbsent(100L));
    }
    @Test
    void cancel_shouldThrow_whenRegistrationNotFound() {
        when(eventRegistrationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.cancel(1L));

        verify(eventRegistrationRepository, never()).save(any());
    }
    @Test
    void markAttended_shouldThrow_whenRegistrationNotFound() {
        when(eventRegistrationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.markAttended(1L));

        verify(eventRepository, never()).findById(anyLong());
    }
}