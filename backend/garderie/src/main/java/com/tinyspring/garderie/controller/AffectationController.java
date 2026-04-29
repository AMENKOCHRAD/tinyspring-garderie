package com.tinyspring.garderie.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import com.tinyspring.garderie.entity.Affectation;
import com.tinyspring.garderie.interfaces.IAffectationService;

import java.util.List;

@RestController
@RequestMapping("/api/affectations")
public class AffectationController {

    private final IAffectationService affectationService;

    public AffectationController(IAffectationService affectationService) {
        this.affectationService = affectationService;
    }

    @PostMapping
    public Affectation addAffectation(@Valid @RequestBody Affectation affectation) {
        return affectationService.addAffectation(affectation);
    }

    @PutMapping("/{id}")
    public Affectation updateAffectation(@PathVariable Long id, @Valid @RequestBody Affectation affectation) {
        affectation.setId(id);
        return affectationService.updateAffectation(affectation);
    }

    @DeleteMapping("/{id}")
    public void deleteAffectation(@PathVariable Long id) {
        affectationService.deleteAffectation(id);
    }

    @GetMapping("/{id}")
    public Affectation getAffectationById(@PathVariable Long id) {
        return affectationService.getAffectationById(id);
    }

    @GetMapping
    public List<Affectation> getAllAffectations() {
        return affectationService.getAllAffectations();
    }
}
