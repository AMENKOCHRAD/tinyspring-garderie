package com.tinyspring.garderie.controller.Events;

import com.tinyspring.garderie.dto.Events.WeeklyMenuRequest;
import com.tinyspring.garderie.dto.Events.WeeklyMenuResponse;
import com.tinyspring.garderie.service.Events.WeeklyMenuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/menus/weekly")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class WeeklyMenuRestController {
    private final WeeklyMenuService weeklyMenuService;

    @GetMapping
    public ResponseEntity<List<WeeklyMenuResponse>> getAll() {
        return ResponseEntity.ok(weeklyMenuService.getAll());
    }

    @PostMapping
    public ResponseEntity<WeeklyMenuResponse> create(@Valid @RequestBody WeeklyMenuRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(weeklyMenuService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WeeklyMenuResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(weeklyMenuService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WeeklyMenuResponse> update(@PathVariable Long id,
                                                     @Valid @RequestBody WeeklyMenuRequest request) {
        return ResponseEntity.ok(weeklyMenuService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        weeklyMenuService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/duplicate")
    public ResponseEntity<WeeklyMenuResponse> duplicate(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(weeklyMenuService.duplicate(id));
    }
}
