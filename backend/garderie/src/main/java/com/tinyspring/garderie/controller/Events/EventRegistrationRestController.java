package com.tinyspring.garderie.controller.Events;

import com.tinyspring.garderie.dto.Events.EventRegistrationRequest;
import com.tinyspring.garderie.entity.Events.EventRegistration;
import com.tinyspring.garderie.service.Events.EventRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class EventRegistrationRestController {

    private final EventRegistrationService eventRegistrationService;

    @PostMapping("/api/events/{id}/registrations")
    public ResponseEntity<EventRegistration> register(@PathVariable("id") Long eventId,
                                                      @Valid @RequestBody EventRegistrationRequest request) {
        EventRegistration registration = eventRegistrationService.register(eventId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(registration);
    }

    @GetMapping("/api/events/{id}/registrations")
    public ResponseEntity<List<EventRegistration>> getByEventId(@PathVariable("id") Long eventId) {
        return ResponseEntity.ok(eventRegistrationService.getByEventId(eventId));
    }

    @PutMapping("/api/registrations/{id}/confirm")
    public ResponseEntity<EventRegistration> confirm(@PathVariable Long id) {
        return ResponseEntity.ok(eventRegistrationService.confirm(id));
    }

    @PutMapping("/api/registrations/{id}/cancel")
    public ResponseEntity<EventRegistration> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(eventRegistrationService.cancel(id));
    }

}
