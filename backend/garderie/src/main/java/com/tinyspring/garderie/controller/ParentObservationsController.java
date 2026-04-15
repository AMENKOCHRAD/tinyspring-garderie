package com.tinyspring.garderie.controller;

import com.tinyspring.garderie.dto.ObservationDto;
import com.tinyspring.garderie.entity.ObservationEnfant;
import com.tinyspring.garderie.service.ParentObservationsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*"})
@RestController
@RequestMapping("/api/parent/observations")
public class ParentObservationsController {

    private final ParentObservationsService service;

    public ParentObservationsController(ParentObservationsService service) {
        this.service = service;
    }

    @GetMapping("/enfant/{enfantId}")
    public ResponseEntity<List<ObservationDto>> lister(@PathVariable Long enfantId, Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        List<ObservationEnfant> observations = service.listerObservationsPourParent(email, enfantId);
        return ResponseEntity.ok(observations.stream().map(this::map).collect(Collectors.toList()));
    }

    private ObservationDto map(ObservationEnfant observation) {
        ObservationDto dto = new ObservationDto();
        dto.setId(observation.getId());
        dto.setType(observation.getType() != null ? observation.getType().name() : null);
        dto.setTitre(observation.getTitre());
        dto.setDescription(observation.getDescription());
        dto.setCreeLe(observation.getCreeLe() != null ? observation.getCreeLe().toString() : null);
        dto.setCreeParNom(observation.getCreePar() != null ? observation.getCreePar().getNom() : null);

        if (observation.getEnfant() != null) {
            dto.setEnfantId(observation.getEnfant().getId());
            dto.setEnfantNom(observation.getEnfant().getNom());
            dto.setEnfantPrenom(observation.getEnfant().getPrenom());
        }

        return dto;
    }
}
