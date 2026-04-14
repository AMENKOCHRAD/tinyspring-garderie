package com.tinyspring.garderie.controller.boutique;

import com.tinyspring.garderie.dto.boutique.CommandeDto;
import com.tinyspring.garderie.dto.boutique.CommandeItemRequest;
import com.tinyspring.garderie.dto.boutique.CommandeRequest;
import com.tinyspring.garderie.service.boutique.CommandeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = {
        "http://localhost:4200",
        "http://localhost:21065"
})
public class CommandeController {

    private final CommandeService commandeService;

    public CommandeController(CommandeService commandeService) {
        this.commandeService = commandeService;
    }

    // ── FRONT-OFFICE ──────────────────────────────────────────────────────────

    // ÉTAPE 1 : Créer la commande (statut PENDING)
    @PostMapping("/api/boutique/commandes")
    public ResponseEntity<?> create(@RequestBody CommandeRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(commandeService.create(request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // ÉTAPE 2 : Créer la session Stripe → retourne { checkoutUrl: "..." }
    @PostMapping("/api/boutique/commandes/{id}/checkout-session")
    public ResponseEntity<?> createCheckoutSession(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(commandeService.createCheckoutSession(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/api/boutique/commandes/user/{userId}")
    public ResponseEntity<List<CommandeDto>> getByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(commandeService.findByUser(userId));
    }

    @GetMapping("/api/boutique/commandes/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(commandeService.findById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // ── BACK-OFFICE ADMIN ─────────────────────────────────────────────────────

    @GetMapping("/api/admin/boutique/commandes")
    public ResponseEntity<List<CommandeDto>> getAll() {
        return ResponseEntity.ok(commandeService.findAll());
    }

    @GetMapping("/api/admin/boutique/commandes/{id}")
    public ResponseEntity<?> getByIdAdmin(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(commandeService.findById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/api/admin/boutique/commandes/statut/{statut}")
    public ResponseEntity<List<CommandeDto>> getByStatut(@PathVariable String statut) {
        return ResponseEntity.ok(commandeService.findByStatut(statut));
    }

    // ✅ Admin change statut manuellement (transitions validées dans le service)
    @PatchMapping("/api/admin/boutique/commandes/{id}/statut")
    public ResponseEntity<?> updateStatut(@PathVariable Long id,
                                          @RequestParam String statut) {
        try {
            return ResponseEntity.ok(commandeService.updateStatut(id, statut));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/api/admin/boutique/commandes/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            commandeService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Impossible de supprimer cette commande."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur : " + e.getMessage()));
        }
    }
}
