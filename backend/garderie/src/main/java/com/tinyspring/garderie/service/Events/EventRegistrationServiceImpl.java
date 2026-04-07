package com.tinyspring.garderie.service.Events;

import com.tinyspring.garderie.dto.Events.EventRegistrationRequest;
import com.tinyspring.garderie.entity.Events.Event;
import com.tinyspring.garderie.entity.Events.EventRegistration;
import com.tinyspring.garderie.entity.Events.EventStatus;
import com.tinyspring.garderie.entity.Events.RegistrationStatus;
import com.tinyspring.garderie.exception.Events.AuthorizationRequiredException;
import com.tinyspring.garderie.exception.Events.InvalidStatusTransitionException;
import com.tinyspring.garderie.exception.Events.ResourceNotFoundException;
import com.tinyspring.garderie.repository.Events.EventRegistrationRepository;
import com.tinyspring.garderie.repository.Events.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class EventRegistrationServiceImpl implements EventRegistrationService {
    private static final Set<RegistrationStatus> CAPACITY_CONSUMING_STATUSES =
            EnumSet.of(RegistrationStatus.CONFIRMED, RegistrationStatus.ATTENDED);
    private static final Set<RegistrationStatus> ACTIVE_REGISTRATION_STATUSES =
            EnumSet.of(RegistrationStatus.PENDING, RegistrationStatus.CONFIRMED, RegistrationStatus.WAITLISTED);

    private final EventRepository eventRepository;
    private final EventRegistrationRepository eventRegistrationRepository;

    @Override
    public EventRegistration register(Long eventId, EventRegistrationRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evenement introuvable avec l'id : " + eventId));

        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new InvalidStatusTransitionException(
                    "Les inscriptions sont autorisees uniquement pour un evenement publie"
            );
        }

        if (eventRegistrationRepository.existsByEventIdAndChildIdAndStatusIn(
                eventId,
                request.getChildId(),
                ACTIVE_REGISTRATION_STATUSES
        )) {
            throw new InvalidStatusTransitionException(
                    "Cet enfant possede deja une inscription active pour cet evenement"
            );
        }

        boolean authorizationSigned = Boolean.TRUE.equals(request.getAuthorizationSigned());
        if (event.isRequiresAuthorization() && !authorizationSigned) {
            throw new AuthorizationRequiredException(
                    "L'autorisation parentale signee est obligatoire pour cet evenement"
            );
        }

        long confirmedCount = eventRegistrationRepository.countByEventIdAndStatusIn(
                eventId,
                CAPACITY_CONSUMING_STATUSES
        );

        RegistrationStatus initialStatus = RegistrationStatus.CONFIRMED;
        if (event.getMaxCapacity() != null && confirmedCount >= event.getMaxCapacity()) {
            initialStatus = RegistrationStatus.WAITLISTED;
        }

        EventRegistration registration = EventRegistration.builder()
                .eventId(eventId)
                .childId(request.getChildId())
                .parentId(request.getParentId())
                .status(initialStatus)
                .authorizationSigned(authorizationSigned)
                .authorizationDocUrl(request.getAuthorizationDocUrl())
                .notes(request.getNotes())
                .build();

        return eventRegistrationRepository.save(registration);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventRegistration> getByEventId(Long eventId) {
        eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evenement introuvable avec l'id : " + eventId));

        return eventRegistrationRepository.findByEventIdOrderByRegisteredAtAsc(eventId);
    }

    @Override
    public EventRegistration confirm(Long registrationId) {
        EventRegistration registration = eventRegistrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Inscription introuvable avec l'id : " + registrationId));

        Event event = eventRepository.findById(registration.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Evenement introuvable avec l'id : " + registration.getEventId()));

        if (registration.getStatus() == RegistrationStatus.CANCELLED) {
            throw new InvalidStatusTransitionException("Impossible de confirmer une inscription annulee");
        }

        if (registration.getStatus() == RegistrationStatus.ATTENDED
                || registration.getStatus() == RegistrationStatus.ABSENT) {
            throw new InvalidStatusTransitionException("Impossible de confirmer une inscription deja cloturee");
        }

        if (event.isRequiresAuthorization() && !registration.isAuthorizationSigned()) {
            throw new AuthorizationRequiredException(
                    "L'autorisation parentale signee est obligatoire avant confirmation"
            );
        }

        long confirmedCount = eventRegistrationRepository.countByEventIdAndStatus(
                event.getId(), RegistrationStatus.CONFIRMED
        );

        if (event.getMaxCapacity() != null
                && confirmedCount >= event.getMaxCapacity()
                && registration.getStatus() != RegistrationStatus.CONFIRMED) {
            registration.setStatus(RegistrationStatus.WAITLISTED);
            return eventRegistrationRepository.save(registration);
        }

        registration.setStatus(RegistrationStatus.CONFIRMED);
        return eventRegistrationRepository.save(registration);
    }

    @Override
    public EventRegistration cancel(Long registrationId) {
        EventRegistration registration = eventRegistrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Inscription introuvable avec l'id : " + registrationId));

        RegistrationStatus oldStatus = registration.getStatus();
        registration.setStatus(RegistrationStatus.CANCELLED);
        EventRegistration cancelled = eventRegistrationRepository.save(registration);

        if (oldStatus == RegistrationStatus.CONFIRMED) {
            eventRegistrationRepository
                    .findFirstByEventIdAndStatusOrderByRegisteredAtAsc(
                            registration.getEventId(),
                            RegistrationStatus.WAITLISTED
                    )
                    .ifPresent(waitlisted -> {
                        if (!waitlisted.isAuthorizationSigned()) {
                            waitlisted.setStatus(RegistrationStatus.PENDING);
                        } else {
                            waitlisted.setStatus(RegistrationStatus.CONFIRMED);
                        }
                        eventRegistrationRepository.save(waitlisted);
                    });
        }

        return cancelled;
    }

    @Override
    public EventRegistration markAttended(Long registrationId) {
        EventRegistration registration = getRegistrationForAttendance(registrationId);
        registration.setStatus(RegistrationStatus.ATTENDED);
        return eventRegistrationRepository.save(registration);
    }

    @Override
    public EventRegistration markAbsent(Long registrationId) {
        EventRegistration registration = getRegistrationForAttendance(registrationId);
        registration.setStatus(RegistrationStatus.ABSENT);
        return eventRegistrationRepository.save(registration);
    }

    private EventRegistration getRegistrationForAttendance(Long registrationId) {
        EventRegistration registration = eventRegistrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Inscription introuvable avec l'id : " + registrationId));

        Event event = eventRepository.findById(registration.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Evenement introuvable avec l'id : " + registration.getEventId()));

        if (event.getStatus() != EventStatus.COMPLETED) {
            throw new InvalidStatusTransitionException(
                    "La presence ne peut etre renseignee que pour un evenement termine"
            );
        }

        if (registration.getStatus() == RegistrationStatus.CANCELLED) {
            throw new InvalidStatusTransitionException(
                    "Une inscription annulee ne peut pas etre marquee presente ou absente"
            );
        }

        return registration;
    }
}
