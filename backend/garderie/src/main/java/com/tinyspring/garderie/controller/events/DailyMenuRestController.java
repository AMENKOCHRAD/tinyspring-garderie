package com.tinyspring.garderie.controller.events;

import com.tinyspring.garderie.dto.Events.DailyMenuRequest;
import com.tinyspring.garderie.dto.Events.DailyMenuResponse;
import com.tinyspring.garderie.service.events.DailyMenuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/menus/daily")
@RequiredArgsConstructor

public class DailyMenuRestController {
    private final DailyMenuService dailyMenuService;

    @PostMapping
    public ResponseEntity<DailyMenuResponse> create(@Valid @RequestBody DailyMenuRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dailyMenuService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DailyMenuResponse> update(@PathVariable Long id,
                                                    @Valid @RequestBody DailyMenuRequest request) {
        return ResponseEntity.ok(dailyMenuService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        dailyMenuService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
