package com.tinyspring.garderie.controller.boutique;

import com.tinyspring.garderie.dto.boutique.ProduitDto;
import com.tinyspring.garderie.service.boutique.FileStorageService;
import com.tinyspring.garderie.service.boutique.ProduitService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
public class ProduitController {

    private final ProduitService produitService;
    private final FileStorageService fileStorageService;

    public ProduitController(ProduitService produitService,
                             FileStorageService fileStorageService) {
        this.produitService = produitService;
        this.fileStorageService = fileStorageService;
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

    // ── BACK-OFFICE ADMIN ─────────────────────────────────────────────────────

    @GetMapping("/api/admin/boutique/produits")
    public ResponseEntity<List<ProduitDto>> getAllAdmin() {
        return ResponseEntity.ok(produitService.findAll());
    }

    @GetMapping("/api/admin/boutique/produits/{id}")
    public ResponseEntity<ProduitDto> getByIdAdmin(@PathVariable Long id) {
        return ResponseEntity.ok(produitService.findById(id));
    }

    // ── CREATE avec upload image ──────────────────────────────────────────────
    @PostMapping(value = "/api/admin/boutique/produits",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProduitDto> create(
            @RequestParam("nom") String nom,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("prix") Double prix,
            @RequestParam("stock") Integer stock,
            @RequestParam("categorieId") Long categorieId,
            @RequestParam(value = "image", required = false) MultipartFile image) {

        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            imageUrl = fileStorageService.saveFile(image);
        }

        ProduitDto dto = ProduitDto.builder()
                .nom(nom)
                .description(description)
                .prix(prix)
                .stock(stock)
                .categorieId(categorieId)
                .imageUrl(imageUrl)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(produitService.create(dto));
    }

    // ── UPDATE avec upload image ──────────────────────────────────────────────
    @PutMapping(value = "/api/admin/boutique/produits/{id}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProduitDto> update(
            @PathVariable Long id,
            @RequestParam("nom") String nom,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("prix") Double prix,
            @RequestParam("stock") Integer stock,
            @RequestParam("categorieId") Long categorieId,
            @RequestParam(value = "image", required = false) MultipartFile image) {

        ProduitDto existing = produitService.findById(id);
        String imageUrl = existing.getImageUrl();

        if (image != null && !image.isEmpty()) {
            fileStorageService.deleteFile(imageUrl);
            imageUrl = fileStorageService.saveFile(image);
        }

        ProduitDto dto = ProduitDto.builder()
                .nom(nom)
                .description(description)
                .prix(prix)
                .stock(stock)
                .categorieId(categorieId)
                .imageUrl(imageUrl)
                .build();

        return ResponseEntity.ok(produitService.update(id, dto));
    }

    // ── DELETE : détache des commandes automatiquement puis supprime ──────────
    @DeleteMapping("/api/admin/boutique/produits/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            ProduitDto existing = produitService.findById(id);
            fileStorageService.deleteFile(existing.getImageUrl());
            produitService.delete(id); // détache + supprime en @Transactional
            return ResponseEntity.noContent().build();

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de la suppression : " + e.getMessage()));
        }
    }

    @GetMapping("/api/admin/boutique/produits/low-stock")
    public ResponseEntity<List<ProduitDto>> getLowStock() {
        return ResponseEntity.ok(produitService.findLowStock());
    }
}
