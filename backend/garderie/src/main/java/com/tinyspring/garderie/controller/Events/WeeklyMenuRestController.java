package com.tinyspring.garderie.controller.Events;

import com.tinyspring.garderie.dto.Events.WeeklyMenuRequest;
import com.tinyspring.garderie.dto.Events.WeeklyMenuResponse;
import com.tinyspring.garderie.service.Events.WeeklyMenuService;
import com.tinyspring.garderie.dto.Events.WeeklyMenuAiGenerateRequest;
import com.tinyspring.garderie.service.Events.MenuAiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.util.stream.Collectors;

import java.util.List;

@RestController
@RequestMapping("/api/menus/weekly")
@RequiredArgsConstructor

public class WeeklyMenuRestController {
    private final WeeklyMenuService weeklyMenuService;
    private final MenuAiService menuAiService;

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



    @GetMapping("/ai/ping")
    public ResponseEntity<String> pingAi() {
        return ResponseEntity.ok("AI endpoint is reachable");
    }
    @PostMapping("/ai/post-test")
    public ResponseEntity<String> postTest() {
        return ResponseEntity.ok("POST works");
    }

    @PostMapping("/ai/echo")
    public ResponseEntity<String> echo(@RequestBody String body) {
        System.out.println("BODY RAW = " + body);
        return ResponseEntity.ok(body);
    }

    @PostMapping("/ai/generate-test")
    public ResponseEntity<String> generateTest(@RequestBody String body) {
        System.out.println("GENERATE TEST BODY = " + body);
        return ResponseEntity.ok("BODY OK");
    }

    @PostMapping("/ai/generate")
    public ResponseEntity<?> generateWithAi(@RequestBody java.util.Map<String, Object> body) {
        try {
            System.out.println("=== generateWithAi called ===");
            System.out.println("RAW BODY = " + body);

            Object rawWeekStartDate = body.get("weekStartDate");
            if (rawWeekStartDate == null || rawWeekStartDate.toString().isBlank()) {
                return ResponseEntity.badRequest().body(java.util.Map.of(
                        "message", "weekStartDate est obligatoire"
                ));
            }

            WeeklyMenuAiGenerateRequest request = new WeeklyMenuAiGenerateRequest();
            request.setWeekStartDate(rawWeekStartDate.toString());

            WeeklyMenuRequest result = menuAiService.generateWeeklyMenuDraft(request);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(java.util.Map.of(
                    "message", "Erreur génération IA",
                    "error", e.getMessage()
            ));
        }
    }


    }



