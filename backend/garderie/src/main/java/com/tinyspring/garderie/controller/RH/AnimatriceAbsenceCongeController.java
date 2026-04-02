package com.tinyspring.garderie.controller.RH;

import com.tinyspring.garderie.dto.RH.AbsenceCongeDTO;
import com.tinyspring.garderie.service.RH.AbsenceCongeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/animatrice/absences-conges")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class AnimatriceAbsenceCongeController {

    private final AbsenceCongeService absenceCongeService;

    @GetMapping("/{animatriceId}")
    public ResponseEntity<List<AbsenceCongeDTO>> getMesAbsenceConges(@PathVariable Long animatriceId) {
        return ResponseEntity.ok(absenceCongeService.getMesAbsenceConges(animatriceId));
    }

    @PostMapping
    public ResponseEntity<AbsenceCongeDTO> soumettreDemande(@Valid @RequestBody AbsenceCongeDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(absenceCongeService.soumettreDemandeAbsenceConge(dto));
    }
}