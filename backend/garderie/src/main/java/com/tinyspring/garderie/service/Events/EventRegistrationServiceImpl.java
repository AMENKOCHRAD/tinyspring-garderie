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

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class EventRegistrationServiceImpl implements EventRegistrationService   {

    private final EventRepository eventRepository;
    private final EventRegistrationRepository eventRegistrationRepository;

    @Override
    public EventRegistration register(Long eventId, EventRegistrationRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Événement introuvable avec l'id : " + eventId));

        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new InvalidStatusTransitionException(
                    "Les inscriptions sont autorisées uniquement pour un événement publié"
            );
        }

        long confirmedCount = eventRegistrationRepository.countByEventIdAndStatus(
                eventId, RegistrationStatus.CONFIRMED
        );

        RegistrationStatus initialStatus;

        // Règle métier :
        // si capacité atteinte -> nouvelle inscription en WAITLISTED.
        if (event.getMaxCapacity() != null && confirmedCount >= event.getMaxCapacity()) {
            initialStatus = RegistrationStatus.WAITLISTED;
        } else {
            // Règle métier :
            // si l'événement exige une autorisation parentale, l'inscription reste PENDING
            // tant que authorizationSigned n'est pas true.
            if (Boolean.TRUE.equals(event.isRequiresAuthorization())) {
                initialStatus = Boolean.TRUE.equals(request.getAuthorizationSigned())
                        ? RegistrationStatus.PENDING
                        : RegistrationStatus.PENDING;
            } else {
                initialStatus = RegistrationStatus.CONFIRMED;
            }
        }

        EventRegistration registration = EventRegistration.builder()
                .eventId(eventId)
                .childId(request.getChildId())
                .parentId(request.getParentId())
                .status(initialStatus)
                .authorizationSigned(Boolean.TRUE.equals(request.getAuthorizationSigned()))
                .authorizationDocUrl(request.getAuthorizationDocUrl())
                .notes(request.getNotes())
                .build();

        return eventRegistrationRepository.save(registration);
    }

    @Override
    public List<EventRegistration> getByEventId(Long eventId) {
        eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Événement introuvable avec l'id : " + eventId));

        return eventRegistrationRepository.findByEventIdOrderByRegisteredAt(eventId);
    }

    @Override
    public EventRegistration confirm(Long registrationId) {
        EventRegistration registration = eventRegistrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Inscription introuvable avec l'id : " + registrationId));

        Event event = eventRepository.findById(registration.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Événement introuvable avec l'id : " + registration.getEventId()));

        if (registration.getStatus() == RegistrationStatus.CANCELLED) {
            throw new InvalidStatusTransitionException("Impossible de confirmer une inscription annulée");
        }

        if (registration.getStatus() == RegistrationStatus.ATTENDED
                || registration.getStatus() == RegistrationStatus.ABSENT) {
            throw new InvalidStatusTransitionException("Impossible de confirmer une inscription déjà clôturée");
        }

        // Règle métier :
        // si l'événement exige une autorisation, la confirmation est bloquée tant que
        // authorizationSigned = false.
        if (event.isRequiresAuthorization() && !registration.isAuthorizationSigned()) {
            throw new AuthorizationRequiredException(
                    "L'autorisation parentale signée est obligatoire avant confirmation"
            );
        }

        long confirmedCount = eventRegistrationRepository.countByEventIdAndStatus(
                event.getId(), RegistrationStatus.CONFIRMED
        );

        // Si la capacité est atteinte, on garde l'inscription en WAITLISTED.
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

        // Règle métier :
        // si une inscription CONFIRMED est annulée, la première WAITLISTED passe en PENDING.
        if (oldStatus == RegistrationStatus.CONFIRMED) {
            eventRegistrationRepository
                    .findFirstByEventIdAndStatusOrderByRegisteredAt(
                            registration.getEventId(),
                            RegistrationStatus.WAITLISTED
                    )
                    .ifPresent(waitlisted -> {
                        waitlisted.setStatus(RegistrationStatus.PENDING);
                        eventRegistrationRepository.save(waitlisted);

                        // Ici on pourra brancher la notification plus tard.
                    });
        }

        return cancelled;
    }

}
