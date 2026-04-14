package com.tinyspring.garderie.controller.RH;

import com.tinyspring.garderie.dto.RH.FormationDTO;
import com.tinyspring.garderie.service.RH.FormationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/animatrice/formations")
@RequiredArgsConstructor
public class AnimatriceFormationController {

    private final FormationService formationService;

    @GetMapping("/disponibles")
    public ResponseEntity<List<FormationDTO>> getFormationsDisponibles() {
        return ResponseEntity.ok(formationService.getFormationsDisponibles());
    }

    @GetMapping("/mes-formations/{animatriceId}")
    public ResponseEntity<List<FormationDTO>> getMesFormations(@PathVariable Long animatriceId) {
        return ResponseEntity.ok(formationService.getMesFormations(animatriceId));
    }

    @PostMapping("/{formationId}/inscrire/{animatriceId}")
    public ResponseEntity<FormationDTO> sInscrire(@PathVariable Long formationId,
                                                  @PathVariable Long animatriceId) {
        return ResponseEntity.ok(formationService.sInscrireFormation(formationId, animatriceId));
    }
}