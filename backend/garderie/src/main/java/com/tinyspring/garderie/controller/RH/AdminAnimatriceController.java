package com.tinyspring.garderie.controller.RH;

import com.tinyspring.garderie.dto.RH.AnimatriceDTO;
import com.tinyspring.garderie.entity.RH.enums.StatutAnimatrice;
import com.tinyspring.garderie.service.RH.AnimatriceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/admin/animatrices")
@RequiredArgsConstructor
public class AdminAnimatriceController {

    private final AnimatriceService animatriceService;

    @GetMapping
    public ResponseEntity<List<AnimatriceDTO>> getAllAnimatrices() {
        return ResponseEntity.ok(animatriceService.getAllAnimatrices());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AnimatriceDTO> getAnimatriceById(@PathVariable Long id) {
        return ResponseEntity.ok(animatriceService.getAnimatriceById(id));
    }

    @GetMapping("/statut/{statut}")
    public ResponseEntity<List<AnimatriceDTO>> getByStatut(@PathVariable StatutAnimatrice statut) {
        return ResponseEntity.ok(animatriceService.getAnimatricesByStatut(statut));
    }

    @PostMapping
    public ResponseEntity<AnimatriceDTO> createAnimatrice(@Valid @RequestBody AnimatriceDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(animatriceService.createAnimatrice(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AnimatriceDTO> updateAnimatrice(@PathVariable Long id,
                                                          @Valid @RequestBody AnimatriceDTO dto) {
        return ResponseEntity.ok(animatriceService.updateAnimatrice(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAnimatrice(@PathVariable Long id) {
        animatriceService.deleteAnimatrice(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/upload-photo")
    public ResponseEntity<AnimatriceDTO> uploadPhoto(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        try {
            AnimatriceDTO updated = animatriceService.uploadPhoto(id, file);
            return ResponseEntity.ok(updated);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}