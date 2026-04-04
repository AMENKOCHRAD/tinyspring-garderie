package com.tinyspring.garderie.service.Events;

import com.tinyspring.garderie.dto.Events.EventRequest;
import com.tinyspring.garderie.entity.Events.Event;


import com.tinyspring.garderie.entity.Events.EventStatus;
import com.tinyspring.garderie.exception.Events.InvalidStatusTransitionException;
import com.tinyspring.garderie.exception.Events.ResourceNotFoundException;
import com.tinyspring.garderie.repository.Events.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService{
    private final EventRepository eventRepository;

    @Override
    public Event create(EventRequest request) {
        Event event = Event.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .type(request.getType())
                .status(request.getStatus() != null ? request.getStatus() : EventStatus.DRAFT)
                .startDatetime(request.getStartDatetime())
                .endDatetime(request.getEndDatetime())
                .location(request.getLocation())
                .maxCapacity(request.getMaxCapacity())
                .requiresAuthorization(Boolean.TRUE.equals(request.getRequiresAuthorization()))
                .priceEvent(request.getPriceEvent())
                .photoEvent(request.getPhotoEvent())
                .classroomId(request.getClassroomId())
                .createdBy(request.getCreatedBy())
                .build();

        validateDates(event);
        return eventRepository.save(event);
    }

    @Override
    public List<Event> getAll() {
        return eventRepository.findAll();
    }

    @Override
    public Event getById(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Événement introuvable avec l'id : " + id));
    }

    @Override
    public Event update(Long id, EventRequest request) {
        Event existing = getById(id);

        // Règle métier : un événement CANCELLED ou COMPLETED ne peut plus être modifié.
        if (existing.getStatus() == EventStatus.CANCELLED || existing.getStatus() == EventStatus.COMPLETED) {
            throw new InvalidStatusTransitionException(
                    "Un événement annulé ou terminé ne peut plus être modifié"
            );
        }

        existing.setTitle(request.getTitle());
        existing.setDescription(request.getDescription());
        existing.setType(request.getType());
        existing.setStartDatetime(request.getStartDatetime());
        existing.setEndDatetime(request.getEndDatetime());
        existing.setLocation(request.getLocation());
        existing.setMaxCapacity(request.getMaxCapacity());
        existing.setRequiresAuthorization(Boolean.TRUE.equals(request.getRequiresAuthorization()));
        existing.setPriceEvent(request.getPriceEvent());
        existing.setPhotoEvent(request.getPhotoEvent());
        existing.setClassroomId(request.getClassroomId());
        existing.setCreatedBy(request.getCreatedBy());

        // On n'autorise pas ici un changement libre de statut métier sensible.
        // La publication passe par publish(id).
        validateDates(existing);

        return eventRepository.save(existing);
    }

    @Override
    public void delete(Long id) {
        Event existing = getById(id);
        eventRepository.delete(existing);
    }

    @Override
    public Event publish(Long id) {
        Event event = getById(id);

        if (event.getStatus() == EventStatus.CANCELLED || event.getStatus() == EventStatus.COMPLETED) {
            throw new InvalidStatusTransitionException(
                    "Impossible de publier un événement annulé ou terminé"
            );
        }

        // Règle métier : publication autorisée seulement si
        // title, startDatetime, endDatetime et classroomId sont renseignés.
        if (isBlank(event.getTitle())
                || event.getStartDatetime() == null
                || event.getEndDatetime() == null
                || event.getClassroomId() == null) {
            throw new InvalidStatusTransitionException(
                    "Impossible de publier l'événement : title, startDatetime, endDatetime et classroomId sont obligatoires"
            );
        }

        validateDates(event);

        event.setStatus(EventStatus.PUBLISHED);
        return eventRepository.save(event);
    }

    private void validateDates(Event event) {
        if (event.getStartDatetime() != null
                && event.getEndDatetime() != null
                && event.getEndDatetime().isBefore(event.getStartDatetime())) {
            throw new InvalidStatusTransitionException(
                    "La date de fin doit être postérieure à la date de début"
            );
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
