package com.tinyspring.garderie.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinyspring.garderie.dto.TraitementValidationEventDto;
import com.tinyspring.garderie.entity.ConditionSanitaire;
import com.tinyspring.garderie.entity.Enfant;
import com.tinyspring.garderie.entity.Traitement;
import com.tinyspring.garderie.entity.TraitementValidationEvent;
import com.tinyspring.garderie.repository.TraitementValidationEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class TraitementValidationHistoryServiceImpl implements TraitementValidationHistoryService {

    private final TraitementValidationEventRepository repository;
    private final ObjectMapper objectMapper;

    public TraitementValidationHistoryServiceImpl(TraitementValidationEventRepository repository,
                                             ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public void recordSystemDecision(Traitement traitement, TraitementAutoValidationService.Result res) {
        if (traitement == null || traitement.getId() == null || res == null || res.decision == null) {
            return;
        }

        TraitementValidationEvent event = new TraitementValidationEvent();
        event.setTraitement(traitement);
        event.setSource(res.source != null ? res.source : "ML");
        event.setDecision(res.decision.name());
        event.setConfiance(res.confiance);
        event.setNote(res.note);
        event.setCreeParEmail("SYSTEM");
        event.setFacteursJson(writeJsonSafe(res.facteurs));
        repository.save(event);
    }

    public void recordAdminDecision(Traitement traitement, String adminEmail, String decision, String note) {
        if (traitement == null || traitement.getId() == null || decision == null || decision.isBlank()) {
            return;
        }

        TraitementValidationEvent event = new TraitementValidationEvent();
        event.setTraitement(traitement);
        event.setSource("ADMIN");
        event.setDecision(decision.trim());
        event.setConfiance(null);
        event.setNote(note);
        event.setCreeParEmail(adminEmail != null ? adminEmail : "ADMIN");
        event.setFacteursJson(null);
        repository.save(event);
    }

    public List<TraitementValidationEventDto> getHistory(Long traitementId) {
        return repository.findTop100ByTraitementIdOrderByCreeLeDesc(traitementId).stream()
                .filter(Objects::nonNull)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<TraitementValidationEventDto> getLatest(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 500));

        return repository.findLatestWithDetails(PageRequest.of(0, safeLimit)).stream()
                .filter(Objects::nonNull)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private TraitementValidationEventDto toDto(TraitementValidationEvent e) {
        TraitementValidationEventDto dto = new TraitementValidationEventDto();
        dto.id = e.getId();

        Traitement t = e.getTraitement();
        if (t != null) {
            dto.traitementId = t.getId();
            dto.statutTraitement = t.getStatut() != null ? t.getStatut().name() : null;
            dto.nomTraitement = t.getNomTraitement();

            ConditionSanitaire cs = t.getConditionSanitaire();
            if (cs != null) {
                dto.nomCondition = cs.getNomCondition();
                dto.typeCondition = cs.getType() != null ? cs.getType().name() : null;

                Enfant enf = cs.getEnfant();
                if (enf != null) {
                    dto.enfantId = enf.getId();
                    dto.nomEnfant = enf.getNom();
                    dto.prenomEnfant = enf.getPrenom();
                    dto.nomParent = enf.getParent() != null ? enf.getParent().getNom() : null;
                }
            }
        }

        dto.decision = e.getDecision();
        dto.source = e.getSource();
        dto.confiance = e.getConfiance();
        dto.note = e.getNote();
        dto.creeLe = e.getCreeLe() != null ? e.getCreeLe().toString() : null;
        dto.creeParEmail = e.getCreeParEmail();
        dto.facteurs = readJsonSafe(e.getFacteursJson());
        return dto;
    }

    private String writeJsonSafe(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return null;
        }
    }

    private Object readJsonSafe(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return objectMapper.readValue(raw, Object.class);
        } catch (Exception ex) {
            return Map.of("raw", raw);
        }
    }
}
