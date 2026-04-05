package com.tinyspring.garderie.controller.Events;

import com.tinyspring.garderie.dto.Events.EventRegistrationRequest;
import com.tinyspring.garderie.dto.Events.EventRegistrationResponse;
import com.tinyspring.garderie.mappeer.EventRegistrationMapper;
import com.tinyspring.garderie.service.Events.EventRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class EventRegistrationRestController {

    private final EventRegistrationService eventRegistrationService;
    private final EventRegistrationMapper eventRegistrationMapper;

    @PostMapping("/events/{id}/registrations")
    public ResponseEntity<EventRegistrationResponse> register(
            @PathVariable("id") Long eventId,
            @Valid @RequestBody EventRegistrationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventRegistrationMapper.toResponse(
                        eventRegistrationService.register(eventId, request)
                ));
    }

    @GetMapping("/events/{id}/registrations")
    public ResponseEntity<List<EventRegistrationResponse>> getByEventId(@PathVariable("id") Long eventId) {
        return ResponseEntity.ok(
                eventRegistrationService.getByEventId(eventId).stream()
                        .map(eventRegistrationMapper::toResponse)
                        .toList()
        );
    }

    @PutMapping("/registrations/{id}/confirm")
    public ResponseEntity<EventRegistrationResponse> confirm(@PathVariable Long id) {
        return ResponseEntity.ok(
                eventRegistrationMapper.toResponse(eventRegistrationService.confirm(id))
        );
    }

    @PutMapping("/registrations/{id}/cancel")
    public ResponseEntity<EventRegistrationResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(
                eventRegistrationMapper.toResponse(eventRegistrationService.cancel(id))
        );
    }
}
