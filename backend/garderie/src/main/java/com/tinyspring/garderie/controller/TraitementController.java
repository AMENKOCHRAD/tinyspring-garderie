package com.tinyspring.garderie.controller;

import com.tinyspring.garderie.dto.TraitementValidationDto;
import com.tinyspring.garderie.entity.Traitement;
import com.tinyspring.garderie.service.TraitementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/traitements")
public class TraitementController {

    private final TraitementService traitementService;

    public TraitementController(TraitementService traitementService) {
        this.traitementService = traitementService;
    }

    @PostMapping("/condition/{conditionId}")
    public ResponseEntity<Traitement> ajouter(@PathVariable Long conditionId,
                                              @RequestBody Traitement traitement) {
        return ResponseEntity.ok(traitementService.ajouterTraitement(conditionId, traitement));
    }

    @GetMapping("/condition/{conditionId}")
    public ResponseEntity<List<Traitement>> listerParCondition(@PathVariable Long conditionId) {
        return ResponseEntity.ok(traitementService.listerTraitementsParCondition(conditionId));
    }

    @GetMapping("/enfant/{enfantId}")
    public ResponseEntity<List<Traitement>> listerParEnfant(@PathVariable Long enfantId) {
        return ResponseEntity.ok(traitementService.listerTraitementsParEnfant(enfantId));
    }

    @PutMapping("/{traitementId}")
    public ResponseEntity<Traitement> modifier(@PathVariable Long traitementId,
                                               @RequestBody Traitement traitement) {
        return ResponseEntity.ok(traitementService.modifierTraitement(traitementId, traitement));
    }

    @DeleteMapping("/{traitementId}")
    public ResponseEntity<Void> supprimer(@PathVariable Long traitementId) {
        traitementService.supprimerTraitement(traitementId);
        return ResponseEntity.noContent().build();
    }

    // ✅ liste des traitements en attente
    @GetMapping("/en-attente-validation")
    public ResponseEntity<List<TraitementValidationDto>> listerEnAttenteValidation() {
        return ResponseEntity.ok(traitementService.listerTraitementsEnAttenteValidation());
    }

    // ✅ consulter détails
    @GetMapping("/details/{traitementId}")
    public ResponseEntity<TraitementValidationDto> consulterDetails(@PathVariable Long traitementId) {
        return ResponseEntity.ok(traitementService.consulterTraitement(traitementId));
    }

    // ✅ valider traitement
    @PutMapping("/valider/{traitementId}")
    public ResponseEntity<Traitement> valider(@PathVariable Long traitementId) {
        return ResponseEntity.ok(traitementService.validerTraitement(traitementId));
    }
}