package com.tinyspring.garderie.controller;

import com.tinyspring.garderie.dto.ObservationCreateDto;
import com.tinyspring.garderie.dto.ObservationDto;
import com.tinyspring.garderie.dto.PriseTraitementCreateDto;
import com.tinyspring.garderie.dto.PriseTraitementDto;
import com.tinyspring.garderie.entity.ObservationEnfant;
import com.tinyspring.garderie.entity.PriseTraitement;
import com.tinyspring.garderie.service.AnimatriceSanteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*"})
@RestController
@RequestMapping("/api/animatrice/sante")
public class AnimatriceSanteController {

    private final AnimatriceSanteService service;

    public AnimatriceSanteController(AnimatriceSanteService service) {
        this.service = service;
    }

    @PostMapping("/traitements/{traitementId}/prises")
    public ResponseEntity<PriseTraitementDto> enregistrerPrise(@PathVariable Long traitementId,
                                                               @RequestBody PriseTraitementCreateDto payload,
                                                               Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(mapPrise(service.enregistrerPrise(email, traitementId, payload)));
    }

    @GetMapping("/enfant/{enfantId}/prises")
    public ResponseEntity<List<PriseTraitementDto>> listerPrises(@PathVariable Long enfantId,
                                                                 @RequestParam(value = "date", required = false) String date) {
        LocalDate parsed = date != null && !date.isBlank() ? LocalDate.parse(date) : null;
        List<PriseTraitement> prises = service.listerPrisesParEnfant(enfantId, parsed);
        return ResponseEntity.ok(prises.stream().map(this::mapPrise).collect(Collectors.toList()));
    }

    @PostMapping("/enfant/{enfantId}/observations")
    public ResponseEntity<ObservationDto> creerObservation(@PathVariable Long enfantId,
                                                           @RequestBody ObservationCreateDto payload,
                                                           Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(mapObservation(service.creerObservation(email, enfantId, payload)));
    }

    @GetMapping("/enfant/{enfantId}/observations")
    public ResponseEntity<List<ObservationDto>> listerObservationsEnfant(@PathVariable Long enfantId) {
        return ResponseEntity.ok(service.listerObservationsEnfant(enfantId).stream()
                .map(this::mapObservation)
                .collect(Collectors.toList()));
    }

    @GetMapping("/observations")
    public ResponseEntity<List<ObservationDto>> listerDernieresObservations() {
        return ResponseEntity.ok(service.listerDernieresObservations().stream()
                .map(this::mapObservation)
                .collect(Collectors.toList()));
    }

    private PriseTraitementDto mapPrise(PriseTraitement prise) {
        PriseTraitementDto dto = new PriseTraitementDto();
        dto.setId(prise.getId());
        dto.setTraitementId(prise.getTraitement().getId());
        dto.setNomTraitement(prise.getTraitement().getNomTraitement());
        dto.setDatePrise(prise.getDatePrise() != null ? prise.getDatePrise().toString() : null);
        dto.setHeurePrevue(prise.getHeurePrevue());
        dto.setDonneLe(prise.getDonneLe() != null ? prise.getDonneLe().toString() : null);
        dto.setDonneParNom(prise.getDonnePar() != null ? prise.getDonnePar().getNom() : null);
        dto.setNote(prise.getNote());

        if (prise.getTraitement().getConditionSanitaire() != null && prise.getTraitement().getConditionSanitaire().getEnfant() != null) {
            dto.setEnfantId(prise.getTraitement().getConditionSanitaire().getEnfant().getId());
            dto.setEnfantNom(prise.getTraitement().getConditionSanitaire().getEnfant().getNom());
            dto.setEnfantPrenom(prise.getTraitement().getConditionSanitaire().getEnfant().getPrenom());
        }

        return dto;
    }

    private ObservationDto mapObservation(ObservationEnfant observation) {
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
