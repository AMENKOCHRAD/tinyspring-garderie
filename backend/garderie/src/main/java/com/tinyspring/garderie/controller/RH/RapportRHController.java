package com.tinyspring.garderie.controller.RH;

import com.tinyspring.garderie.dto.RH.RapportRHDTO;
import com.tinyspring.garderie.dto.RH.RapportRequestDTO;
import com.tinyspring.garderie.service.RH.RapportRHService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/rapports")
@RequiredArgsConstructor
public class RapportRHController {

    private final RapportRHService rapportRHService;

    // ✅ Générer un nouveau rapport via langage naturel
    @PostMapping("/generer")
    public ResponseEntity<RapportRHDTO> genererRapport(
            @RequestBody RapportRequestDTO request) {
        RapportRHDTO rapport = rapportRHService.genererRapport(request.getQuestion());
        return ResponseEntity.ok(rapport);
    }

    // ✅ Récupérer tous les rapports générés
    @GetMapping
    public ResponseEntity<List<RapportRHDTO>> getTousLesRapports() {
        return ResponseEntity.ok(rapportRHService.getTousLesRapports());
    }

    // ✅ Récupérer un rapport par ID
    @GetMapping("/{id}")
    public ResponseEntity<RapportRHDTO> getRapportById(@PathVariable Long id) {
        return ResponseEntity.ok(rapportRHService.getRapportById(id));
    }
}