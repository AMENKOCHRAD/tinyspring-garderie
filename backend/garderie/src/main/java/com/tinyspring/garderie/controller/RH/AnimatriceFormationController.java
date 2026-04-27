package com.tinyspring.garderie.controller.RH;

import com.tinyspring.garderie.entity.RH.AnimatriceFormation;
import com.tinyspring.garderie.entity.RH.Formation;
import com.tinyspring.garderie.service.RH.IFormationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/animatrice/formations")
@RequiredArgsConstructor
public class AnimatriceFormationController {

    private final IFormationService formationService;

    // GET /api/animatrice/formations
    @GetMapping
    public ResponseEntity<List<Formation>> getFormationsDisponibles() {
        return ResponseEntity.ok(formationService.getToutesFormations());
    }

    // GET /api/animatrice/formations/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Formation> getFormationById(@PathVariable Long id) {
        return ResponseEntity.ok(formationService.getFormationById(id));
    }

    // GET /api/animatrice/formations/profil/{animatriceId}
    @GetMapping("/profil/{animatriceId}")
    public ResponseEntity<Map<String, Object>> getMonProfil(
            @PathVariable Long animatriceId) {
        return ResponseEntity.ok(formationService.getProfilFormations(animatriceId));
    }

    // GET /api/animatrice/formations/suggestions/{animatriceId}
    @GetMapping("/suggestions/{animatriceId}")
    public ResponseEntity<List<Map<String, Object>>> getMesSuggestions(
            @PathVariable Long animatriceId) {
        return ResponseEntity.ok(formationService.getSuggestions(animatriceId));
    }

    // GET /api/animatrice/formations/alertes/{animatriceId}
    @GetMapping("/alertes/{animatriceId}")
    public ResponseEntity<Map<String, Object>> getMesAlertes(
            @PathVariable Long animatriceId) {
        return ResponseEntity.ok(formationService.getAlertesAnimatrice(animatriceId));
    }

    // POST /api/animatrice/formations/{formationId}/inscrire
    @PostMapping("/{formationId}/inscrire")
    public ResponseEntity<?> sInscrire(
            @PathVariable Long formationId,
            @RequestBody Map<String, Long> body) {
        try {
            Long animatriceId = body.get("animatriceId");
            AnimatriceFormation result = formationService.inscrireAnimatrice(formationId, animatriceId);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // DELETE /api/animatrice/formations/{formationId}/desinscrire/{animatriceId}
    @DeleteMapping("/{formationId}/desinscrire/{animatriceId}")
    public ResponseEntity<?> seDesinscrire(
            @PathVariable Long formationId,
            @PathVariable Long animatriceId) {
        try {
            formationService.desinscrireAnimatrice(formationId, animatriceId);
            return ResponseEntity.ok(Map.of("message", "✅ Désinscription effectuée"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }
}