package com.tinyspring.garderie.controller.boutique;

import com.tinyspring.garderie.dto.boutique.CommandeDto;
import com.tinyspring.garderie.dto.boutique.CommandeRequest;
import com.tinyspring.garderie.service.boutique.CommandeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
public class CommandeController {

    private final CommandeService commandeService;

    public CommandeController(CommandeService commandeService) {
        this.commandeService = commandeService;
    }

    // ── FRONT-OFFICE (authentifié) ────────────────────────────────────────────

    // Créer une commande (tout user connecté)
    @PostMapping("/api/boutique/commandes")
    public ResponseEntity<CommandeDto> create(@RequestBody CommandeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commandeService.create(request));
    }

    // Voir ses propres commandes
    @GetMapping("/api/boutique/commandes/user/{userId}")
    public ResponseEntity<List<CommandeDto>> getByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(commandeService.findByUser(userId));
    }

    // Voir le détail d'une commande
    @GetMapping("/api/boutique/commandes/{id}")
    public ResponseEntity<CommandeDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(commandeService.findById(id));
    }

    // ── BACK-OFFICE ADMIN (protégé par SecurityConfig → ROLE_ADMIN) ───────────

    // Toutes les commandes
    @GetMapping("/api/admin/boutique/commandes")
    public ResponseEntity<List<CommandeDto>> getAll() {
        return ResponseEntity.ok(commandeService.findAll());
    }

    @GetMapping("/api/admin/boutique/commandes/{id}")
    public ResponseEntity<CommandeDto> getByIdAdmin(@PathVariable Long id) {
        return ResponseEntity.ok(commandeService.findById(id));
    }

    // Filtrer par statut
    @GetMapping("/api/admin/boutique/commandes/statut/{statut}")
    public ResponseEntity<List<CommandeDto>> getByStatut(@PathVariable String statut) {
        return ResponseEntity.ok(commandeService.findByStatut(statut));
    }

    // Mettre à jour le statut d'une commande
    @PatchMapping("/api/admin/boutique/commandes/{id}/statut")
    public ResponseEntity<CommandeDto> updateStatut(@PathVariable Long id,
                                                    @RequestParam String statut) {
        return ResponseEntity.ok(commandeService.updateStatut(id, statut));
    }

    // Supprimer une commande
    @DeleteMapping("/api/admin/boutique/commandes/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        commandeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
