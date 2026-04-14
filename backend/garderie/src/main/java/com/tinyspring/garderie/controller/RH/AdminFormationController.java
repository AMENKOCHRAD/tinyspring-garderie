package com.tinyspring.garderie.controller.RH;

import com.tinyspring.garderie.dto.RH.FormationDTO;
import com.tinyspring.garderie.entity.RH.enums.StatutFormation;
import com.tinyspring.garderie.service.RH.FormationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/formations")
@RequiredArgsConstructor
public class AdminFormationController {

    private final FormationService formationService;

    @GetMapping
    public ResponseEntity<List<FormationDTO>> getAllFormations() {
        return ResponseEntity.ok(formationService.getAllFormations());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FormationDTO> getFormationById(@PathVariable Long id) {
        return ResponseEntity.ok(formationService.getFormationById(id));
    }

    @PostMapping
    public ResponseEntity<FormationDTO> createFormation(@Valid @RequestBody FormationDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(formationService.createFormation(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FormationDTO> updateFormation(@PathVariable Long id,
                                                        @Valid @RequestBody FormationDTO dto) {
        return ResponseEntity.ok(formationService.updateFormation(id, dto));
    }

    @PutMapping("/{id}/statut/{statut}")
    public ResponseEntity<FormationDTO> updateStatut(@PathVariable Long id,
                                                     @PathVariable StatutFormation statut) {
        return ResponseEntity.ok(formationService.updateStatutFormation(id, statut));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFormation(@PathVariable Long id) {
        formationService.deleteFormation(id);
        return ResponseEntity.noContent().build();
    }
}