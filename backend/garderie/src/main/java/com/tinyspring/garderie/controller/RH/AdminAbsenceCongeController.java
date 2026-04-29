package com.tinyspring.garderie.controller.RH;

import com.tinyspring.garderie.dto.RH.AbsenceCongeDTO;
import com.tinyspring.garderie.entity.RH.enums.StatutAbsenceConge;
import com.tinyspring.garderie.service.RH.IAbsenceCongeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/absences-conges")
@RequiredArgsConstructor
public class AdminAbsenceCongeController {

    private final IAbsenceCongeService absenceCongeService;

    @GetMapping
    public ResponseEntity<List<AbsenceCongeDTO>> getAllAbsenceConges() {
        return ResponseEntity.ok(absenceCongeService.getAllAbsenceConges());
    }

    // ✅ AJOUT — Détail d'une absence par ID
    @GetMapping("/{id}")
    public ResponseEntity<AbsenceCongeDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(absenceCongeService.getAbsenceCongeById(id));
    }

    @GetMapping("/statut/{statut}")
    public ResponseEntity<List<AbsenceCongeDTO>> getByStatut(@PathVariable StatutAbsenceConge statut) {
        return ResponseEntity.ok(absenceCongeService.getAbsenceCongesByStatut(statut));
    }

    @PutMapping("/{id}/valider")
    public ResponseEntity<AbsenceCongeDTO> valider(@PathVariable Long id) {
        return ResponseEntity.ok(absenceCongeService.validerDemande(id));
    }

    @PutMapping("/{id}/refuser")
    public ResponseEntity<AbsenceCongeDTO> refuser(@PathVariable Long id) {
        return ResponseEntity.ok(absenceCongeService.refuserDemande(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        absenceCongeService.deleteAbsenceConge(id);
        return ResponseEntity.noContent().build();
    }
}