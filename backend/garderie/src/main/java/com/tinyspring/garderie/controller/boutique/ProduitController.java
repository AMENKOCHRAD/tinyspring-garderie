package com.tinyspring.garderie.controller.boutique;

import com.tinyspring.garderie.dto.boutique.ProduitDto;
import com.tinyspring.garderie.service.boutique.ProduitService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
public class ProduitController {

    private final ProduitService produitService;

    public ProduitController(ProduitService produitService) {
        this.produitService = produitService;
    }

    // ── FRONT-OFFICE (public) ─────────────────────────────────────────────────

    @GetMapping("/api/boutique/produits")
    public ResponseEntity<List<ProduitDto>> getAllPublic() {
        return ResponseEntity.ok(produitService.findAll());
    }

    @GetMapping("/api/boutique/produits/{id}")
    public ResponseEntity<ProduitDto> getByIdPublic(@PathVariable Long id) {
        return ResponseEntity.ok(produitService.findById(id));
    }

    @GetMapping("/api/boutique/produits/categorie/{categorieId}")
    public ResponseEntity<List<ProduitDto>> getByCategorie(@PathVariable Long categorieId) {
        return ResponseEntity.ok(produitService.findByCategorie(categorieId));
    }

    @GetMapping("/api/boutique/produits/search")
    public ResponseEntity<List<ProduitDto>> search(@RequestParam String nom) {
        return ResponseEntity.ok(produitService.search(nom));
    }

    @GetMapping("/api/boutique/produits/en-stock")
    public ResponseEntity<List<ProduitDto>> getEnStock() {
        return ResponseEntity.ok(produitService.findEnStock());
    }

    // ── BACK-OFFICE ADMIN (protégé par SecurityConfig → ROLE_ADMIN) ───────────

    @GetMapping("/api/admin/boutique/produits")
    public ResponseEntity<List<ProduitDto>> getAllAdmin() {
        return ResponseEntity.ok(produitService.findAll());
    }

    @GetMapping("/api/admin/boutique/produits/{id}")
    public ResponseEntity<ProduitDto> getByIdAdmin(@PathVariable Long id) {
        return ResponseEntity.ok(produitService.findById(id));
    }

    @PostMapping("/api/admin/boutique/produits")
    public ResponseEntity<ProduitDto> create(@RequestBody ProduitDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(produitService.create(dto));
    }

    @PutMapping("/api/admin/boutique/produits/{id}")
    public ResponseEntity<ProduitDto> update(@PathVariable Long id,
                                             @RequestBody ProduitDto dto) {
        return ResponseEntity.ok(produitService.update(id, dto));
    }

    @DeleteMapping("/api/admin/boutique/produits/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        produitService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
