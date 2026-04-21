package com.tinyspring.garderie.service.Events;

import com.tinyspring.garderie.dto.Events.EventRequest;
import com.tinyspring.garderie.dto.Events.EventResponse;
import com.tinyspring.garderie.entity.Events.Event;
import com.tinyspring.garderie.entity.Events.EventRating;
import com.tinyspring.garderie.entity.Events.EventStatus;
import com.tinyspring.garderie.exception.Events.InvalidStatusTransitionException;
import com.tinyspring.garderie.exception.Events.ResourceNotFoundException;
import com.tinyspring.garderie.mappeer.EventMapper;
import com.tinyspring.garderie.repository.Classes.ClasseRepository;
import com.tinyspring.garderie.repository.Events.EventRatingRepository;
import com.tinyspring.garderie.repository.Events.EventRegistrationRepository;
import com.tinyspring.garderie.repository.Events.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {
    private static final Path EVENT_UPLOAD_DIRECTORY =
            Paths.get("uploads", "events").toAbsolutePath().normalize();

    private final ClasseRepository classeRepository;
    private final EventRepository eventRepository;
    private final EventRegistrationRepository eventRegistrationRepository;
    private final EventMapper eventMapper;
    private final EventRatingRepository eventRatingRepository;

    @Override
    public Event create(EventRequest request) {
        validateClassroomReferences(request.getClassroomId(), request.getTargetClassroomIds());

        Event event = eventMapper.toEntity(request);
        event.setStatus(request.getStatus() != null ? request.getStatus() : EventStatus.DRAFT);
        event.setRequiresAuthorization(Boolean.TRUE.equals(request.getRequiresAuthorization()));
        event.setEventPrice(request.getEventPrice());

        validateDates(event);
        validateLocation(event);
        return eventRepository.save(event);
    }

    @Override
    public List<EventResponse> getAllWithRatings() {
        List<Event> events = eventRepository.findAll();
        events.forEach(this::syncCompletedStatusIfNeeded);

        return events.stream()
                .sorted((e1, e2) -> {
                    int statusCompare = Integer.compare(
                            getStatusPriority(e1.getStatus()),
                            getStatusPriority(e2.getStatus())
                    );

                    if (statusCompare != 0) {
                        return statusCompare;
                    }

                    if (e1.getStartDatetime() == null && e2.getStartDatetime() == null) return 0;
                    if (e1.getStartDatetime() == null) return 1;
                    if (e2.getStartDatetime() == null) return -1;

                    return e1.getStartDatetime().compareTo(e2.getStartDatetime());
                })
                .map(event -> {

                    EventResponse response = eventMapper.toResponse(event);

                    // 🔥 ICI ON AJOUTE LE RATING
                    List<EventRating> ratings =
                            eventRatingRepository.findByEventId(event.getId());

                    response.setRatingCount((long) ratings.size());

                    double avg = ratings.stream()
                            .mapToInt(EventRating::getStars)
                            .average()
                            .orElse(0.0);

                    response.setAverageRating(avg);

                    return response;
                })
                .toList();
    }

    @Override
    public List<Event> getAll() {
        List<Event> events = eventRepository.findAll();
        events.forEach(this::syncCompletedStatusIfNeeded);

        return events.stream()
                .sorted((e1, e2) -> {
                    int statusCompare = Integer.compare(
                            getStatusPriority(e1.getStatus()),
                            getStatusPriority(e2.getStatus())
                    );

                    if (statusCompare != 0) {
                        return statusCompare;
                    }

                    if (e1.getStartDatetime() == null && e2.getStartDatetime() == null) {
                        return 0;
                    }
                    if (e1.getStartDatetime() == null) {
                        return 1;
                    }
                    if (e2.getStartDatetime() == null) {
                        return -1;
                    }

                    return e1.getStartDatetime().compareTo(e2.getStartDatetime());
                })
                .toList();
    }

    @Override
    public List<Event> getPublished() {
        List<Event> events = eventRepository.findByStatusOrderByStartDatetimeAsc(EventStatus.PUBLISHED);
        return events.stream()
                .map(this::syncCompletedStatusIfNeeded)
                .filter(event -> event.getStatus() == EventStatus.PUBLISHED)
                .toList();
    }

    @Override
    public Event getById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evenement introuvable avec l'id : " + id));
        return syncCompletedStatusIfNeeded(event);
    }

    @Override
    public Event getPublishedById(Long id) {
        Event event = getById(id);
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new ResourceNotFoundException("Evenement public introuvable avec l'id : " + id);
        }

        return event;
    }

    @Override
    public Event update(Long id, EventRequest request) {
        Event existing = getById(id);

        if (existing.getStatus() == EventStatus.CANCELLED) {
            throw new InvalidStatusTransitionException(
                    "Un evenement annule ne peut plus etre modifie"
            );
        }

        validateClassroomReferences(request.getClassroomId(), request.getTargetClassroomIds());

        EventStatus previousStatus = existing.getStatus();

        eventMapper.updateEntityFromRequest(request, existing);
        existing.setRequiresAuthorization(Boolean.TRUE.equals(request.getRequiresAuthorization()));
        existing.setEventPrice(request.getEventPrice());

        validateDates(existing);
        validateLocation(existing);

        if (previousStatus == EventStatus.COMPLETED && isRepublishableAfterEdit(existing)) {
            existing.setStatus(EventStatus.DRAFT);
        } else if (request.getStatus() != null) {
            existing.setStatus(request.getStatus());
        }


        return eventRepository.save(existing);
    }

    @Override
    public void delete(Long id) {
        Event existing = getById(id);

        if (eventRegistrationRepository.existsByEventId(existing.getId())) {
            throw new InvalidStatusTransitionException(
                    "Impossible de supprimer cet evenement car des participations y sont deja rattachees. Annulez l'evenement a la place si vous souhaitez le retirer des parcours actifs."
            );
        }

        eventRepository.delete(existing);
    }

    @Override
    public Event publish(Long id) {
        Event event = getById(id);

        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new InvalidStatusTransitionException(
                    "Un evenement annule ne peut pas etre publie"
            );
        }

        if (event.getStatus() == EventStatus.COMPLETED && !isRepublishableAfterEdit(event)) {
            throw new InvalidStatusTransitionException(
                    "Un evenement termine doit avoir une nouvelle date de fin dans le futur avant d'etre republie"
            );
        }

        if (isBlank(event.getTitle())
                || event.getStartDatetime() == null
                || event.getEndDatetime() == null
                || event.getClassroomId() == null) {
            throw new InvalidStatusTransitionException(
                    "Impossible de publier l'evenement : title, startDatetime, endDatetime et classroomId sont obligatoires"
            );
        }

        validateClassroomReferences(
                event.getClassroomId(),
                eventMapper.deserializeClassroomIds(event.getTargetClassroomIds())
        );
        validateDates(event);

        event.setStatus(EventStatus.PUBLISHED);
        return eventRepository.save(event);
    }

    @Override
    public Event uploadPhoto(Long id, MultipartFile file) {
        Event event = getById(id);

        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new InvalidStatusTransitionException(
                    "Un evenement annule ne peut plus etre modifie"
            );
        }

        if (file == null || file.isEmpty()) {
            throw new InvalidStatusTransitionException("Le fichier image est obligatoire");
        }

        try {
            Files.createDirectories(EVENT_UPLOAD_DIRECTORY);

            String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
            String extension = extractExtension(originalFilename);
            String uniqueFilename = UUID.randomUUID() + extension;
            Path targetFile = EVENT_UPLOAD_DIRECTORY.resolve(uniqueFilename).normalize();

            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);

            event.setPhotoEvent("/uploads/events/" + uniqueFilename);
            return eventRepository.save(event);
        } catch (IOException exception) {
            throw new InvalidStatusTransitionException("Impossible d'enregistrer l'image de l'evenement");
        }
    }

    private void validateDates(Event event) {
        if (event.getStartDatetime() != null
                && event.getEndDatetime() != null
                && event.getEndDatetime().isBefore(event.getStartDatetime())) {
            throw new InvalidStatusTransitionException(
                    "La date de fin doit etre posterieure a la date de debut"
            );
        }
    }

    private boolean isRepublishableAfterEdit(Event event) {
        LocalDateTime now = LocalDateTime.now();
        return event.getEndDatetime() != null && event.getEndDatetime().isAfter(now);
    }

    private Event syncCompletedStatusIfNeeded(Event event) {
        if (event.getStatus() == EventStatus.CANCELLED || event.getStatus() == EventStatus.COMPLETED) {
            return event;
        }

        if (event.getEndDatetime() != null && event.getEndDatetime().isBefore(LocalDateTime.now())) {
            event.setStatus(EventStatus.COMPLETED);
            return eventRepository.save(event);
        }

        return event;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String extractExtension(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            return "";
        }

        return filename.substring(filename.lastIndexOf('.'));
    }

    public void validateLocation(Event event) {
        boolean hasLat = event.getLatitude() != null;
        boolean hasLng = event.getLongitude() != null;

        if (hasLat != hasLng) {
            throw new InvalidStatusTransitionException(
                    "Latitude et longitude doivent etre fournies ensemble"
            );
        }
    }

    private void validateClassroomReferences(Long classroomId, List<Long> targetClassroomIds) {
        validateClassroomExists(classroomId, "La classe principale selectionnee est introuvable");

        if (targetClassroomIds == null || targetClassroomIds.isEmpty()) {
            return;
        }

        targetClassroomIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .forEach(targetId -> validateClassroomExists(
                        targetId,
                        "La classe ciblee avec l'id " + targetId + " est introuvable"
                ));
    }

    private void validateClassroomExists(Long classroomId, String errorMessage) {
        if (classroomId == null || !classeRepository.existsById(classroomId)) {
            throw new InvalidStatusTransitionException(errorMessage);
        }
    }


    public int getStatusPriority(EventStatus status) {
        return switch (status) {
            case PUBLISHED -> 1;
            case CANCELLED -> 2;
            case COMPLETED -> 3;
            case DRAFT -> 4;
            default -> 99;
        };
    }


}
