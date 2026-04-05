package com.tinyspring.garderie.controller.boutique;

import com.tinyspring.garderie.dto.boutique.CategorieDto;
import com.tinyspring.garderie.service.boutique.CategorieService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
public class CategorieController {

    private final CategorieService categorieService;

    public CategorieController(CategorieService categorieService) {
        this.categorieService = categorieService;
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

    // ── BACK-OFFICE ADMIN (protégé par SecurityConfig → ROLE_ADMIN) ───────────

    @GetMapping("/api/admin/boutique/categories")
    public ResponseEntity<List<CategorieDto>> getAllAdmin() {
        return ResponseEntity.ok(categorieService.findAll());
    }

    @GetMapping("/api/admin/boutique/categories/{id}")
    public ResponseEntity<CategorieDto> getByIdAdmin(@PathVariable Long id) {
        return ResponseEntity.ok(categorieService.findById(id));
    }

    @PostMapping("/api/admin/boutique/categories")
    public ResponseEntity<CategorieDto> create(@RequestBody CategorieDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categorieService.create(dto));
    }

    @PutMapping("/api/admin/boutique/categories/{id}")
    public ResponseEntity<CategorieDto> update(@PathVariable Long id,
                                               @RequestBody CategorieDto dto) {
        return ResponseEntity.ok(categorieService.update(id, dto));
    }

    @DeleteMapping("/api/admin/boutique/categories/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categorieService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
