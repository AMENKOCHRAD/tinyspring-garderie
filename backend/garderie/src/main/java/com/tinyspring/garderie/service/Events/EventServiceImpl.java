package com.tinyspring.garderie.service.Events;

import com.tinyspring.garderie.dto.Events.EventRequest;
import com.tinyspring.garderie.entity.Events.Event;
import com.tinyspring.garderie.entity.Events.EventStatus;
import com.tinyspring.garderie.exception.Events.InvalidStatusTransitionException;
import com.tinyspring.garderie.exception.Events.ResourceNotFoundException;
import com.tinyspring.garderie.mappeer.EventMapper;
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
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {
    private static final Path EVENT_UPLOAD_DIRECTORY =
            Paths.get("uploads", "events").toAbsolutePath().normalize();

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;

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
                .eventPrice(request.getEventPrice())
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
                .orElseThrow(() -> new ResourceNotFoundException("Evenement introuvable avec l'id : " + id));
    }

    @Override
    public Event update(Long id, EventRequest request) {
        Event existing = getById(id);

        if (existing.getStatus() == EventStatus.CANCELLED || existing.getStatus() == EventStatus.COMPLETED) {
            throw new InvalidStatusTransitionException(
                    "Un evenement annule ou termine ne peut plus etre modifie"
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
        existing.setEventPrice(request.getEventPrice());
        existing.setPhotoEvent(request.getPhotoEvent());
        existing.setClassroomId(request.getClassroomId());
        existing.setCreatedBy(request.getCreatedBy());

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
                    "Impossible de publier un evenement annule ou termine"
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

        validateDates(event);

        event.setStatus(EventStatus.PUBLISHED);
        return eventRepository.save(event);
    }

    @Override
    public Event uploadPhoto(Long id, MultipartFile file) {
        Event event = getById(id);

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

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String extractExtension(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            return "";
        }

        return filename.substring(filename.lastIndexOf('.'));
    }
}
