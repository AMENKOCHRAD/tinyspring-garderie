package com.tinyspring.garderie.controller.Parent;

import com.tinyspring.garderie.dto.Events.*;
import com.tinyspring.garderie.dto.Parent.ParentChildResponse;
import com.tinyspring.garderie.service.Parent.ParentPortalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/parent")
public class ParentPortalController {

    private final ParentPortalService parentPortalService;

    @GetMapping("/children")
    public ResponseEntity<List<ParentChildResponse>> getChildren(@RequestParam Long parentId) {
        return ResponseEntity.ok(parentPortalService.getChildren(parentId));
    }

    @GetMapping("/events")
    public ResponseEntity<List<EventResponse>> getEvents(@RequestParam Long parentId) {
        return ResponseEntity.ok(parentPortalService.getEvents(parentId));
    }

    @GetMapping("/event-registrations")
    public ResponseEntity<List<EventRegistrationResponse>> getParticipations(@RequestParam Long parentId) {
        return ResponseEntity.ok(parentPortalService.getParticipations(parentId));
    }

    @PostMapping(value = "/events/{eventId}/participations", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EventRegistrationResponse> participate(
            @PathVariable Long eventId,
            @RequestParam Long parentId,
            @RequestParam Long childId,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) MultipartFile authorizationFile
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                parentPortalService.participate(parentId, eventId, childId, notes, authorizationFile)
        );
    }

    @PutMapping("/event-registrations/{registrationId}/cancel")
    public ResponseEntity<EventRegistrationResponse> cancelParticipation(
            @PathVariable Long registrationId,
            @RequestParam Long parentId
    ) {
        return ResponseEntity.ok(parentPortalService.cancelParticipation(parentId, registrationId));
    }

    @PostMapping("/events/{eventId}/rating")
    public ResponseEntity<EventRatingResponse> rateEvent(
            @PathVariable Long eventId,
            @RequestParam Long parentId,
            @Valid @RequestBody EventRatingRequest request
    ) {
        return ResponseEntity.ok(parentPortalService.rateEvent(parentId, eventId, request));
    }
    @GetMapping("/menus")
    public ResponseEntity<List<WeeklyMenuResponse>> getMenus(@RequestParam Long parentId) {
        return ResponseEntity.ok(parentPortalService.getMenus(parentId));
    }
}
