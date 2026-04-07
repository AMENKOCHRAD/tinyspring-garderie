package com.tinyspring.garderie.controller;

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

    // Ajouter un traitement à une condition sanitaire
    @PostMapping("/condition/{conditionId}")
    public ResponseEntity<Traitement> ajouter(@PathVariable Long conditionId,
                                              @RequestBody Traitement traitement) {
        return ResponseEntity.ok(traitementService.ajouterTraitement(conditionId, traitement));
    }

    // Lister tous les traitements d'une condition sanitaire
    @GetMapping("/condition/{conditionId}")
    public ResponseEntity<List<Traitement>> listerParCondition(@PathVariable Long conditionId) {
        return ResponseEntity.ok(traitementService.listerTraitementsParCondition(conditionId));
    }

    // Lister tous les traitements d'un enfant via ses conditions
    @GetMapping("/enfant/{enfantId}")
    public ResponseEntity<List<Traitement>> listerParEnfant(@PathVariable Long enfantId) {
        return ResponseEntity.ok(traitementService.listerTraitementsParEnfant(enfantId));
    }

    // Modifier un traitement
    @PutMapping("/{traitementId}")
    public ResponseEntity<Traitement> modifier(@PathVariable Long traitementId,
                                               @RequestBody Traitement traitement) {
        return ResponseEntity.ok(traitementService.modifierTraitement(traitementId, traitement));
    }

    // Supprimer un traitement
    @DeleteMapping("/{traitementId}")
    public ResponseEntity<Void> supprimer(@PathVariable Long traitementId) {
        traitementService.supprimerTraitement(traitementId);
        return ResponseEntity.noContent().build();
    }
}