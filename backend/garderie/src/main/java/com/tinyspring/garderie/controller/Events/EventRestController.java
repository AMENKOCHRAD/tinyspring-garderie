package com.tinyspring.garderie.controller.Events;

import com.tinyspring.garderie.dto.Events.EventRequest;
import com.tinyspring.garderie.dto.Events.EventResponse;
import com.tinyspring.garderie.mappeer.EventMapper;
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
    private final EventMapper eventMapper;

    @PostMapping
    public ResponseEntity<EventResponse> create(@Valid @RequestBody EventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventMapper.toResponse(eventService.create(request)));
    }

    @GetMapping
    public ResponseEntity<List<EventResponse>> getAll() {
        return ResponseEntity.ok(
                eventService.getAll().stream().map(eventMapper::toResponse).toList()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(eventMapper.toResponse(eventService.getById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventResponse> update(@PathVariable Long id,
                                                @Valid @RequestBody EventRequest request) {
        return ResponseEntity.ok(eventMapper.toResponse(eventService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        eventService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/publish")
    public ResponseEntity<EventResponse> publish(@PathVariable Long id) {
        return ResponseEntity.ok(eventMapper.toResponse(eventService.publish(id)));
    }

    @PostMapping("/{id}/photo")
    public ResponseEntity<EventResponse> uploadPhoto(@PathVariable Long id,
                                                     @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(eventMapper.toResponse(eventService.uploadPhoto(id, file)));
    }

}
