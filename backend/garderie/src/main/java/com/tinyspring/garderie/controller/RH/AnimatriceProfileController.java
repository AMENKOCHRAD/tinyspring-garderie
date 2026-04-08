package com.tinyspring.garderie.controller.RH;

import com.tinyspring.garderie.dto.RH.AnimatriceDTO;
import com.tinyspring.garderie.service.RH.AnimatriceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/animatrice/profil")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4201"})
public class AnimatriceProfileController {

    private final AnimatriceService animatriceService;

    @GetMapping("/{id}")
    public ResponseEntity<AnimatriceDTO> getMonProfil(@PathVariable Long id) {
        return ResponseEntity.ok(animatriceService.getAnimatriceById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AnimatriceDTO> updateMonProfil(@PathVariable Long id,
                                                         @Valid @RequestBody AnimatriceDTO dto) {
        return ResponseEntity.ok(animatriceService.updateMonProfil(id, dto));
    }

    // ✅ NOUVEL ENDPOINT — récupérer animatrice par email
    @GetMapping("/par-email")
    public ResponseEntity<AnimatriceDTO> getByEmail(@RequestParam String email) {
        return ResponseEntity.ok(animatriceService.getAnimatriceByEmail(email));
    }
}