package com.tinyspring.garderie.controller;

import com.tinyspring.garderie.dto.EnfantDTO;
import com.tinyspring.garderie.entity.Enfant;
import com.tinyspring.garderie.service.EnfantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import com.tinyspring.garderie.dto.EnfantResponseDTO;


@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/enfants")
public class EnfantController {

    private final EnfantService enfantService;

    public EnfantController(EnfantService enfantService) {
        this.enfantService = enfantService;
    }

    @PostMapping
    public Enfant ajouterEnfant(@RequestBody EnfantDTO dto) {
        return enfantService.ajouterEnfant(dto);
    }

    @GetMapping("/parent/{parentId}")
    public List<Enfant> getEnfantsParParent(@PathVariable Long parentId) {
        return enfantService.getEnfantsParParent(parentId);
    }

    @GetMapping("/{id}")
    public Optional<Enfant> getEnfant(@PathVariable Long id) {
        return enfantService.getEnfantParId(id);
    }

    @PutMapping("/{id}")
    public Enfant modifierEnfant(@PathVariable Long id, @RequestBody EnfantDTO dto) {
        return enfantService.modifierEnfant(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimerEnfant(@PathVariable Long id) {
        enfantService.supprimerEnfant(id);
        return ResponseEntity.noContent().build(); // retourne 204
    }
    @GetMapping
    public List<EnfantResponseDTO> getAllEnfants() {
        return enfantService.getAllEnfants();
    }
}