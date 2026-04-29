package com.tinyspring.garderie.controller;

import com.tinyspring.garderie.dto.TraitementCreateDto;
import com.tinyspring.garderie.dto.TraitementValidationEventDto;
import com.tinyspring.garderie.dto.TraitementValidationDto;
import com.tinyspring.garderie.entity.Traitement;
import com.tinyspring.garderie.service.TraitementService;
import com.tinyspring.garderie.service.TraitementValidationHistoryService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.List;

@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*"})
@RestController
@RequestMapping("/api/traitements")
public class TraitementController {

    private final TraitementService traitementService;
    private final TraitementValidationHistoryService validationHistoryService;

    public TraitementController(TraitementService traitementService,
                                TraitementValidationHistoryService validationHistoryService) {
        this.traitementService = traitementService;
        this.validationHistoryService = validationHistoryService;
    }

    @PostMapping("/condition/{conditionId}")
    public ResponseEntity<Traitement> ajouter(@PathVariable Long conditionId,
                                              @RequestBody Traitement traitement) {
        return ResponseEntity.ok(traitementService.ajouterTraitement(conditionId, traitement));
    }

    @PostMapping(value = "/condition/{conditionId}/avec-ordonnance", consumes = {"multipart/form-data"})
    public ResponseEntity<Traitement> ajouterAvecOrdonnance(@PathVariable Long conditionId,
                                                           @RequestPart("payload") TraitementCreateDto payload,
                                                           @RequestPart("ordonnancePdf") MultipartFile ordonnancePdf) {
        String storedPdfFilename = traitementService.enregistrerOrdonnancePdf(ordonnancePdf);
        return ResponseEntity.ok(traitementService.ajouterTraitementAvecOrdonnance(conditionId, payload, storedPdfFilename));
    }

    @GetMapping("/condition/{conditionId}")
    public ResponseEntity<List<Traitement>> listerParCondition(@PathVariable Long conditionId) {
        return ResponseEntity.ok(traitementService.listerTraitementsParCondition(conditionId));
    }

    @GetMapping("/enfant/{enfantId}")
    public ResponseEntity<List<Traitement>> listerParEnfant(@PathVariable Long enfantId) {
        return ResponseEntity.ok(traitementService.listerTraitementsParEnfant(enfantId));
    }

    @PutMapping("/{traitementId}")
    public ResponseEntity<Traitement> modifier(@PathVariable Long traitementId,
                                               @RequestBody Traitement traitement) {
        return ResponseEntity.ok(traitementService.modifierTraitement(traitementId, traitement));
    }

    @DeleteMapping("/{traitementId}")
    public ResponseEntity<Void> supprimer(@PathVariable Long traitementId) {
        traitementService.supprimerTraitement(traitementId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/en-attente-validation")
    public ResponseEntity<List<TraitementValidationDto>> listerEnAttenteValidation() {
        return ResponseEntity.ok(traitementService.listerTraitementsEnAttenteValidation());
    }

    @GetMapping("/details/{traitementId}")
    public ResponseEntity<TraitementValidationDto> consulterDetails(@PathVariable Long traitementId) {
        return ResponseEntity.ok(traitementService.consulterTraitement(traitementId));
    }

    @PutMapping("/valider/{traitementId}")
    public ResponseEntity<Traitement> valider(@PathVariable Long traitementId) {
        return ResponseEntity.ok(traitementService.validerTraitement(traitementId));
    }

    @PutMapping("/refuser/{traitementId}")
    public ResponseEntity<Traitement> refuser(@PathVariable Long traitementId,
                                              @RequestParam(value = "note", required = false) String note,
                                              Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(traitementService.refuserTraitementAdmin(traitementId, email, note));
    }

    @GetMapping("/{traitementId}/validation-history")
    public ResponseEntity<List<TraitementValidationEventDto>> history(@PathVariable Long traitementId) {
        return ResponseEntity.ok(validationHistoryService.getHistory(traitementId));
    }

    @GetMapping("/validation-events")
    public ResponseEntity<List<TraitementValidationEventDto>> latestEvents(
            @RequestParam(value = "limit", required = false, defaultValue = "200") int limit) {
        return ResponseEntity.ok(validationHistoryService.getLatest(limit));
    }

    @GetMapping("/{traitementId}/ordonnance")
    public ResponseEntity<Resource> telechargerOrdonnance(@PathVariable Long traitementId,
                                                          Authentication authentication) {
        boolean isAdmin = hasRole(authentication != null ? authentication.getAuthorities() : null, "ROLE_ADMIN");
        String email = authentication != null ? authentication.getName() : null;

        Resource resource = isAdmin
                ? traitementService.chargerOrdonnanceResourceAdmin(traitementId)
                : traitementService.chargerOrdonnanceResourceParParent(email, traitementId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"ordonnance-" + traitementId + ".pdf\"")
                .body(resource);
    }

    private boolean hasRole(Collection<? extends GrantedAuthority> authorities, String role) {
        if (authorities == null || role == null) {
            return false;
        }
        return authorities.stream().anyMatch(a -> role.equals(a.getAuthority()));
    }
}
