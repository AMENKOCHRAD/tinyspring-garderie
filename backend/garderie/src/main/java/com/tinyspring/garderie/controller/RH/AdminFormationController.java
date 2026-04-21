package com.tinyspring.garderie.controller.RH;

import com.tinyspring.garderie.entity.RH.AnimatriceFormation;
import com.tinyspring.garderie.entity.RH.Formation;
import com.tinyspring.garderie.service.RH.FormationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/formations")
@RequiredArgsConstructor
public class AdminFormationController {

    private final FormationService formationService;

    // ===== CRUD =====

    // GET /api/admin/formations
    @GetMapping
    public ResponseEntity<List<Formation>> getToutesFormations() {
        return ResponseEntity.ok(formationService.getToutesFormations());
    }

    // GET /api/admin/formations/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Formation> getFormationById(@PathVariable Long id) {
        return ResponseEntity.ok(formationService.getFormationById(id));
    }

    // POST /api/admin/formations
    @PostMapping
    public ResponseEntity<Formation> creerFormation(@RequestBody Formation formation) {
        return ResponseEntity.ok(formationService.creerFormation(formation));
    }

    // PUT /api/admin/formations/{id}
    @PutMapping("/{id}")
    public ResponseEntity<Formation> modifierFormation(
            @PathVariable Long id, @RequestBody Formation formation) {
        return ResponseEntity.ok(formationService.modifierFormation(id, formation));
    }

    // DELETE /api/admin/formations/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimerFormation(@PathVariable Long id) {
        formationService.supprimerFormation(id);
        return ResponseEntity.noContent().build();
    }

    // ===== CYCLE DE VIE =====

    // POST /api/admin/formations/{id}/demarrer
    @PostMapping("/{id}/demarrer")
    public ResponseEntity<Formation> demarrerFormation(@PathVariable Long id) {
        return ResponseEntity.ok(formationService.demarrerFormation(id));
    }

    // POST /api/admin/formations/{id}/terminer
    @PostMapping("/{id}/terminer")
    public ResponseEntity<Formation> terminerFormation(@PathVariable Long id) {
        return ResponseEntity.ok(formationService.terminerFormation(id));
    }

    // POST /api/admin/formations/{id}/annuler
    @PostMapping("/{id}/annuler")
    public ResponseEntity<Formation> annulerFormation(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String motif = body.getOrDefault("motif", "Annulation administrative");
        return ResponseEntity.ok(formationService.annulerFormation(id, motif));
    }

    // ===== INSCRIPTIONS =====

    // GET /api/admin/formations/{id}/inscriptions
    @GetMapping("/{id}/inscriptions")
    public ResponseEntity<List<AnimatriceFormation>> getInscriptions(@PathVariable Long id) {
        return ResponseEntity.ok(
                formationService.getFormationById(id).getInscriptions()
        );
    }

    // POST /api/admin/formations/{id}/inscrire/{animatriceId}
    @PostMapping("/{id}/inscrire/{animatriceId}")
    public ResponseEntity<AnimatriceFormation> inscrireAnimatrice(
            @PathVariable Long id,
            @PathVariable Long animatriceId) {
        return ResponseEntity.ok(formationService.inscrireAnimatrice(id, animatriceId));
    }

    // DELETE /api/admin/formations/{id}/desinscrire/{animatriceId}
    @DeleteMapping("/{id}/desinscrire/{animatriceId}")
    public ResponseEntity<Map<String, String>> desinscrireAnimatrice(
            @PathVariable Long id,
            @PathVariable Long animatriceId) {
        formationService.desinscrireAnimatrice(id, animatriceId);
        return ResponseEntity.ok(Map.of("message", "✅ Animatrice désinscrite"));
    }

    // ===== PROFIL & ALERTES =====

    // GET /api/admin/formations/animatrices/{animatriceId}/profil
    @GetMapping("/animatrices/{animatriceId}/profil")
    public ResponseEntity<Map<String, Object>> getProfilFormations(
            @PathVariable Long animatriceId) {
        return ResponseEntity.ok(formationService.getProfilFormations(animatriceId));
    }

    // GET /api/admin/formations/animatrices/{animatriceId}/suggestions
    @GetMapping("/animatrices/{animatriceId}/suggestions")
    public ResponseEntity<List<Map<String, Object>>> getSuggestions(
            @PathVariable Long animatriceId) {
        return ResponseEntity.ok(formationService.getSuggestions(animatriceId));
    }

    // GET /api/admin/formations/alertes
    @GetMapping("/alertes")
    public ResponseEntity<Map<String, Object>> getAlertesGlobales() {
        return ResponseEntity.ok(formationService.getAlertesGlobales());
    }

    // GET /api/admin/formations/stats
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(formationService.getStatsFormations());
    }
}