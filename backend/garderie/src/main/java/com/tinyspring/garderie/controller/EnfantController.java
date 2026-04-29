package com.tinyspring.garderie.controller;

import com.tinyspring.garderie.dto.EnfantDTO;
import com.tinyspring.garderie.dto.EnfantResponseDTO;
import com.tinyspring.garderie.entity.Enfant;
import com.tinyspring.garderie.service.EnfantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*"})
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
    public EnfantResponseDTO getEnfant(@PathVariable Long id) {
        return enfantService.getEnfantDTOById(id);
    }

    @PutMapping("/{id}")
    public Enfant modifierEnfant(@PathVariable Long id, @RequestBody EnfantDTO dto) {
        return enfantService.modifierEnfant(id, dto);
    }

    @PutMapping("/{id}/archiver")
    public ResponseEntity<Enfant> archiverEnfant(@PathVariable Long id) {
        return ResponseEntity.ok(enfantService.archiverEnfant(id));
    }

    @GetMapping
    public List<EnfantResponseDTO> getAllEnfants() {
        return enfantService.getAllEnfants();
    }
}
