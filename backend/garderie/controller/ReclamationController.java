package com.tinyspring.garderie.controller;

import com.tinyspring.garderie.dto.CreateReclamationRequest;
import com.tinyspring.garderie.dto.UpdateReclamationRequest;
import com.tinyspring.garderie.dto.UpdateReclamationStatusRequest;
import com.tinyspring.garderie.entity.Reclamation;
import com.tinyspring.garderie.service.ReclamationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reclamations")
public class ReclamationController {

    private final ReclamationService reclamationService;

    public ReclamationController(ReclamationService reclamationService) {
        this.reclamationService = reclamationService;
    }

    @PostMapping
    public Reclamation createReclamation(@RequestBody CreateReclamationRequest request) {
        return reclamationService.createReclamation(request);
    }

    @GetMapping
    public List<Reclamation> getMyReclamations() {
        return reclamationService.getMyReclamations();
    }

    @GetMapping("/{id}")
    public Reclamation getReclamation(@PathVariable Long id) {
        return reclamationService.getReclamationById(id);
    }

    @PutMapping("/{id}")
    public Reclamation updateReclamation(@PathVariable Long id,
                                         @RequestBody UpdateReclamationRequest request) {
        return reclamationService.updateReclamation(id, request);
    }

    @PutMapping("/{id}/status")
    public Reclamation updateStatus(@PathVariable Long id,
                                    @RequestBody UpdateReclamationStatusRequest request) {
        return reclamationService.updateStatus(id, request);
    }

    @DeleteMapping("/{id}")
    public void deleteReclamation(@PathVariable Long id) {
        reclamationService.deleteReclamation(id);
    }
}