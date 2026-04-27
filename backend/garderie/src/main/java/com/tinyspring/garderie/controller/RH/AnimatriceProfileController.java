package com.tinyspring.garderie.controller.RH;

import com.tinyspring.garderie.dto.RH.AnimatriceDTO;
import com.tinyspring.garderie.service.RH.IAnimatriceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/animatrice/profil")
@RequiredArgsConstructor
public class AnimatriceProfileController {

    private final IAnimatriceService animatriceService;

    // ✅ Récupérer son profil par ID
    @GetMapping("/{id}")
    public ResponseEntity<AnimatriceDTO> getMonProfil(@PathVariable Long id) {
        return ResponseEntity.ok(animatriceService.getAnimatriceById(id));
    }

    // ✅ Modifier son profil
    @PutMapping("/{id}")
    public ResponseEntity<AnimatriceDTO> updateMonProfil(@PathVariable Long id,
                                                         @Valid @RequestBody AnimatriceDTO dto) {
        return ResponseEntity.ok(animatriceService.updateMonProfil(id, dto));
    }

    // ✅ Récupérer animatrice par email (utilisé par le frontoffice au login)
    @GetMapping("/par-email")
    public ResponseEntity<AnimatriceDTO> getByEmail(@RequestParam String email) {
        return ResponseEntity.ok(animatriceService.getAnimatriceByEmail(email));
    }

    // ✅ NOUVEAU — Upload photo de profil par l'animatrice elle-même
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