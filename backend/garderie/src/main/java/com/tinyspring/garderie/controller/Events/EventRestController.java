package com.tinyspring.garderie.controller.Events;

import com.tinyspring.garderie.dto.Events.EventRequest;
import com.tinyspring.garderie.dto.Events.EventResponse;
import com.tinyspring.garderie.entity.Events.Event;
import com.tinyspring.garderie.entity.Events.RegistrationStatus;
import com.tinyspring.garderie.mappeer.EventMapper;
import com.tinyspring.garderie.repository.Events.EventRegistrationRepository;
import com.tinyspring.garderie.service.Events.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.EnumSet;
import java.util.Set;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventRestController {
    private static final Set<RegistrationStatus> CAPACITY_CONSUMING_STATUSES =
            EnumSet.of(RegistrationStatus.CONFIRMED, RegistrationStatus.ATTENDED);

    private final EventService eventService;
    private final EventMapper eventMapper;
    private final EventRegistrationRepository eventRegistrationRepository;

    @PostMapping
    public ResponseEntity<EventResponse> create(@Valid @RequestBody EventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventMapper.toResponse(eventService.create(request)));
    }

    @GetMapping
    public ResponseEntity<List<EventResponse>> getAll() {
        return ResponseEntity.ok(
                eventService.getAll().stream().map(this::toEventResponse).toList()
        );
    }

    @GetMapping("/public")
    public ResponseEntity<List<EventResponse>> getPublished() {
        return ResponseEntity.ok(
                eventService.getPublished().stream().map(this::toEventResponse).toList()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(toEventResponse(eventService.getById(id)));
    }

    @GetMapping("/public/{id}")
    public ResponseEntity<EventResponse> getPublishedById(@PathVariable Long id) {
        return ResponseEntity.ok(toEventResponse(eventService.getPublishedById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventResponse> update(@PathVariable Long id,
                                                @Valid @RequestBody EventRequest request) {
        return ResponseEntity.ok(toEventResponse(eventService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        eventService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/publish")
    public ResponseEntity<EventResponse> publish(@PathVariable Long id) {
        return ResponseEntity.ok(toEventResponse(eventService.publish(id)));
    }

    @PostMapping("/{id}/photo")
    public ResponseEntity<EventResponse> uploadPhoto(@PathVariable Long id,
                                                     @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(toEventResponse(eventService.uploadPhoto(id, file)));
    }

    private EventResponse toEventResponse(Event event) {
        EventResponse response = eventMapper.toResponse(event);
        long confirmedRegistrations = eventRegistrationRepository.countByEventIdAndStatusIn(
                event.getId(),
                CAPACITY_CONSUMING_STATUSES
        );
        long waitlistedRegistrations = eventRegistrationRepository.countByEventIdAndStatus(
                event.getId(),
                RegistrationStatus.WAITLISTED
        );
        Integer remainingCapacity = event.getMaxCapacity() == null
                ? null
                : Math.max(event.getMaxCapacity() - Math.toIntExact(confirmedRegistrations), 0);

        response.setConfirmedRegistrations(confirmedRegistrations);
        response.setWaitlistedRegistrations(waitlistedRegistrations);
        response.setRemainingCapacity(remainingCapacity);
        response.setFull(remainingCapacity != null && remainingCapacity == 0);
        response.setRegistrationOpen(!response.isFull());
        return response;
    }
}
