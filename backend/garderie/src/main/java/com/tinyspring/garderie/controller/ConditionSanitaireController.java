package com.tinyspring.garderie.controller;

import com.tinyspring.garderie.entity.ConditionSanitaire;
import com.tinyspring.garderie.service.ConditionSanitaireService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*"})
@RestController
@RequestMapping("/api/conditions")
public class ConditionSanitaireController {

    private final ConditionSanitaireService service;

    public ConditionSanitaireController(ConditionSanitaireService service) {
        this.service = service;
    }

    @PostMapping("/enfant/{enfantId}")
    public ResponseEntity<ConditionSanitaire> addCondition(@PathVariable Long enfantId,
                                                           @RequestBody ConditionSanitaire condition) {
        return ResponseEntity.ok(service.addCondition(enfantId, condition));
    }

    @GetMapping("/enfant/{enfantId}")
    public ResponseEntity<List<ConditionSanitaire>> getConditions(@PathVariable Long enfantId) {
        return ResponseEntity.ok(service.getConditionsByEnfant(enfantId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ConditionSanitaire> updateCondition(@PathVariable Long id,
                                                              @RequestBody ConditionSanitaire condition) {
        return ResponseEntity.ok(service.updateCondition(id, condition));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCondition(@PathVariable Long id) {
        service.deleteCondition(id);
        return ResponseEntity.noContent().build();
    }
}
