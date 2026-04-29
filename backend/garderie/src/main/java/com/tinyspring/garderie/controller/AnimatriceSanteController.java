package com.tinyspring.garderie.controller;

import com.tinyspring.garderie.dto.ObservationCreateDto;
import com.tinyspring.garderie.dto.ObservationDto;
import com.tinyspring.garderie.dto.ObservationAiRequestDto;
import com.tinyspring.garderie.dto.ObservationAiResponseDto;
import com.tinyspring.garderie.dto.PriseTraitementCreateDto;
import com.tinyspring.garderie.dto.PriseTraitementDto;
import com.tinyspring.garderie.entity.ObservationEnfant;
import com.tinyspring.garderie.entity.PriseTraitement;
import com.tinyspring.garderie.service.AnimatriceSanteService;
import com.tinyspring.garderie.service.ObservationAiService;
import com.tinyspring.garderie.service.DuplicateObservationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*"})
@RestController
@RequestMapping("/api/animatrice/sante")
public class AnimatriceSanteController {

    private final AnimatriceSanteService service;
    private final ObservationAiService observationAiService;

    public AnimatriceSanteController(AnimatriceSanteService service,
                                    ObservationAiService observationAiService) {
        this.service = service;
        this.observationAiService = observationAiService;
    }

    @PostMapping("/traitements/{traitementId}/prises")
    public ResponseEntity<?> enregistrerPrise(@PathVariable Long traitementId,
                                              @RequestBody PriseTraitementCreateDto payload,
                                              Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        try {
            return ResponseEntity.ok(mapPrise(service.enregistrerPrise(email, traitementId, payload)));
        } catch (RuntimeException exception) {
            String message = exception.getMessage() != null ? exception.getMessage() : "Requete invalide.";
            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("message", message));
        }
    }

    @GetMapping("/enfant/{enfantId}/prises")
    public ResponseEntity<List<PriseTraitementDto>> listerPrises(@PathVariable Long enfantId,
                                                                 @RequestParam(value = "date", required = false) String date,
                                                                 @RequestParam(value = "from", required = false) String from,
                                                                 @RequestParam(value = "to", required = false) String to) {
        LocalDate parsedDate = date != null && !date.isBlank() ? LocalDate.parse(date) : null;

        LocalDate parsedFrom = from != null && !from.isBlank() ? LocalDate.parse(from) : null;
        LocalDate parsedTo = to != null && !to.isBlank() ? LocalDate.parse(to) : null;

        List<PriseTraitement> prises;
        if (parsedFrom != null || parsedTo != null) {
            prises = service.listerPrisesParEnfantPeriode(enfantId, parsedFrom, parsedTo);
        } else {
            prises = service.listerPrisesParEnfant(enfantId, parsedDate);
        }

        return ResponseEntity.ok(prises.stream().map(this::mapPrise).collect(Collectors.toList()));
    }

    @GetMapping("/prises")
    public ResponseEntity<List<PriseTraitementDto>> listerToutesPrises(@RequestParam(value = "date", required = false) String date,
                                                                       Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        LocalDate parsed = date != null && !date.isBlank() ? LocalDate.parse(date) : null;
        List<PriseTraitement> prises = service.listerToutesPrises(email, parsed);
        return ResponseEntity.ok(prises.stream().map(this::mapPrise).collect(Collectors.toList()));
    }

    @GetMapping("/mes-prises")
    public ResponseEntity<List<PriseTraitementDto>> listerMesPrises(@RequestParam(value = "from", required = false) String from,
                                                                    @RequestParam(value = "to", required = false) String to,
                                                                    Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        LocalDate parsedFrom = from != null && !from.isBlank() ? LocalDate.parse(from) : null;
        LocalDate parsedTo = to != null && !to.isBlank() ? LocalDate.parse(to) : null;
        List<PriseTraitement> prises = service.listerMesPrises(email, parsedFrom, parsedTo);
        return ResponseEntity.ok(prises.stream().map(this::mapPrise).collect(Collectors.toList()));
    }

    @GetMapping("/prises/{priseId}/pdf")
    public ResponseEntity<?> telechargerPdfPrise(@PathVariable Long priseId, Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        try {
            byte[] pdf = service.genererPdfPrise(email, priseId);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"prise-traitement-" + priseId + ".pdf\"")
                    .body(pdf);
        } catch (RuntimeException exception) {
            String message = exception.getMessage() != null ? exception.getMessage() : "Requete invalide.";
            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("message", message));
        }
    }

    @PostMapping("/enfant/{enfantId}/observations")
    public ResponseEntity<?> creerObservation(@PathVariable Long enfantId,
                                              @RequestBody ObservationCreateDto payload,
                                              Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        try {
            return ResponseEntity.ok(mapObservation(service.creerObservation(email, enfantId, payload)));
        } catch (DuplicateObservationException exception) {
            String message = exception.getMessage() != null ? exception.getMessage() : "Doublon detecte.";
            return ResponseEntity.status(409)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "message", message,
                            "duplicateOfId", exception.getDuplicateOfId(),
                            "score", exception.getScore()
                    ));
        } catch (RuntimeException exception) {
            String message = exception.getMessage() != null ? exception.getMessage() : "Requete invalide.";
            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("message", message));
        }
    }

    @PostMapping("/observations/suggestion")
    public ResponseEntity<ObservationAiResponseDto> suggererObservation(@RequestBody ObservationAiRequestDto payload,
                                                                       Authentication authentication) {
        // L'endpoint est protege par role ANIMATRICE dans SecurityConfig.
        // Pas besoin d'email ici, mais on garde Authentication pour coherence (et futur audit).
        return ResponseEntity.ok(observationAiService.generer(payload));
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
        dto.setLuParent(observation.isLuParent());
        dto.setLuLe(observation.getLuLe() != null ? observation.getLuLe().toString() : null);
        dto.setObserveLe(observation.getObserveLe() != null ? observation.getObserveLe().toString() : null);
        dto.setUrgence(observation.getUrgence() != null ? observation.getUrgence().name() : null);
        dto.setTemperature(observation.getTemperature());
        dto.setLieu(observation.getLieu());
        dto.setSymptomes(observation.getSymptomes());
        dto.setActionsEffectuees(observation.getActionsEffectuees());

        if (observation.getEnfant() != null) {
            dto.setEnfantId(observation.getEnfant().getId());
            dto.setEnfantNom(observation.getEnfant().getNom());
            dto.setEnfantPrenom(observation.getEnfant().getPrenom());
        }

        return dto;
    }
}
