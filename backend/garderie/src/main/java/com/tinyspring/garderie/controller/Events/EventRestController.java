package com.tinyspring.garderie.controller.Events;

import com.tinyspring.garderie.dto.Events.EventRequest;
import com.tinyspring.garderie.entity.Events.Event;
import com.tinyspring.garderie.service.Events.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventRestController {
    private final EventService eventService;

    @PostMapping
    public ResponseEntity<Event> create(@Valid @RequestBody EventRequest request) {
        Event createdEvent = eventService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdEvent);
    }

    @GetMapping
    public ResponseEntity<List<Event>> getAll() {
        return ResponseEntity.ok(eventService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Event> getById(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Event> update(@PathVariable Long id,
                                        @Valid @RequestBody EventRequest request) {
        return ResponseEntity.ok(eventService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        eventService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/publish")
    public ResponseEntity<Event> publish(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.publish(id));
    }

    @PostMapping("/{id}/photo")
    public ResponseEntity<Event> uploadPhoto(@PathVariable Long id,
                                             @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(eventService.uploadPhoto(id, file));
    }

}
