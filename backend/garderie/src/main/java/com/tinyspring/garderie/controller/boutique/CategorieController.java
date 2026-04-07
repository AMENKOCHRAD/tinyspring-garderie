package com.tinyspring.garderie.controller.boutique;

import com.tinyspring.garderie.dto.boutique.CategorieDto;
import com.tinyspring.garderie.service.boutique.CategorieService;
import com.tinyspring.garderie.service.boutique.FileStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
public class CategorieController {

    private final CategorieService categorieService;
    private final FileStorageService fileStorageService;

    public CategorieController(CategorieService categorieService,
                               FileStorageService fileStorageService) {
        this.categorieService = categorieService;
        this.fileStorageService = fileStorageService;
    }

    // ── FRONT-OFFICE (public) ─────────────────────────────────────────────────

    @GetMapping("/api/boutique/categories")
    public ResponseEntity<List<CategorieDto>> getAllPublic() {
        return ResponseEntity.ok(categorieService.findAll());
    }

    @GetMapping("/api/boutique/categories/{id}")
    public ResponseEntity<CategorieDto> getByIdPublic(@PathVariable Long id) {
        return ResponseEntity.ok(categorieService.findById(id));
    }

    // ── BACK-OFFICE ADMIN ─────────────────────────────────────────────────────

    @GetMapping("/api/admin/boutique/categories")
    public ResponseEntity<List<CategorieDto>> getAllAdmin() {
        return ResponseEntity.ok(categorieService.findAll());
    }

    @GetMapping("/api/admin/boutique/categories/{id}")
    public ResponseEntity<CategorieDto> getByIdAdmin(@PathVariable Long id) {
        return ResponseEntity.ok(categorieService.findById(id));
    }

    // ── CREATE avec upload image (multipart/form-data) ────────────────────────
    @PostMapping(value = "/api/admin/boutique/categories",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CategorieDto> create(
            @RequestParam("nom") String nom,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "image", required = false) MultipartFile image) {

        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            imageUrl = fileStorageService.saveFile(image);
        }

        CategorieDto dto = CategorieDto.builder()
                .nom(nom)
                .description(description)
                .imageUrl(imageUrl)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(categorieService.create(dto));
    }

    // ── UPDATE avec upload image (multipart/form-data) ────────────────────────
    @PutMapping(value = "/api/admin/boutique/categories/{id}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CategorieDto> update(
            @PathVariable Long id,
            @RequestParam("nom") String nom,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "image", required = false) MultipartFile image) {

        CategorieDto existing = categorieService.findById(id);
        String imageUrl = existing.getImageUrl();

        if (image != null && !image.isEmpty()) {
            fileStorageService.deleteFile(imageUrl);
            imageUrl = fileStorageService.saveFile(image);
        }

        CategorieDto dto = CategorieDto.builder()
                .nom(nom)
                .description(description)
                .imageUrl(imageUrl)
                .build();

        return ResponseEntity.ok(categorieService.update(id, dto));
    }

    // ── DELETE avec gestion contrainte produits ───────────────────────────────
    @DeleteMapping("/api/admin/boutique/categories/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            CategorieDto existing = categorieService.findById(id);

            // Vérifier si la catégorie a des produits liés
            if (existing.getNombreProduits() > 0) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of(
                                "message", "Impossible de supprimer : cette catégorie contient "
                                        + existing.getNombreProduits() + " produit(s). "
                                        + "Supprimez ou déplacez les produits d'abord."
                        ));
            }

            fileStorageService.deleteFile(existing.getImageUrl());
            categorieService.delete(id);
            return ResponseEntity.noContent().build();

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de la suppression : " + e.getMessage()));
        }
    }
}
