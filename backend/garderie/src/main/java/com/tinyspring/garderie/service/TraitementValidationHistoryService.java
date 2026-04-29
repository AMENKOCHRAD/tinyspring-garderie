package com.tinyspring.garderie.service;

import com.tinyspring.garderie.dto.TraitementValidationEventDto;
import com.tinyspring.garderie.entity.Traitement;

import java.util.List;

public interface TraitementValidationHistoryService {

    void recordSystemDecision(Traitement traitement, TraitementAutoValidationService.Result res);

    void recordAdminDecision(Traitement traitement, String adminEmail, String decision, String note);

    List<TraitementValidationEventDto> getHistory(Long traitementId);

    List<TraitementValidationEventDto> getLatest(int limit);
}

