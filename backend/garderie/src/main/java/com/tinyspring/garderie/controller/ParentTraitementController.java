package com.tinyspring.garderie.controller;

import com.tinyspring.garderie.dto.TraitementUpdateDto;
import com.tinyspring.garderie.entity.Traitement;
import com.tinyspring.garderie.service.TraitementService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*"})
@RestController
@RequestMapping("/api/parent/traitements")
public class ParentTraitementController {

    private final TraitementService traitementService;

    public ParentTraitementController(TraitementService traitementService) {
        this.traitementService = traitementService;
    }

    @PutMapping("/{traitementId}")
    public ResponseEntity<Traitement> modifier(@PathVariable Long traitementId,
                                               @RequestBody TraitementUpdateDto payload,
                                               Authentication authentication) {
        String emailParent = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(traitementService.modifierTraitementParParent(emailParent, traitementId, payload));
    }

    @PutMapping("/{traitementId}/annuler")
    public ResponseEntity<Traitement> annuler(@PathVariable Long traitementId,
                                              Authentication authentication) {
        String emailParent = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(traitementService.annulerTraitementParParent(emailParent, traitementId));
    }

    @DeleteMapping("/{traitementId}")
    public ResponseEntity<Void> supprimer(@PathVariable Long traitementId,
                                          Authentication authentication) {
        String emailParent = authentication != null ? authentication.getName() : null;
        traitementService.supprimerTraitementParParent(emailParent, traitementId);
        return ResponseEntity.noContent().build();
    }
}
