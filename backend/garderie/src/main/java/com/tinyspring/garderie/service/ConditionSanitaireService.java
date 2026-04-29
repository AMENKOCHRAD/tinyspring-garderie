package com.tinyspring.garderie.service;

import com.tinyspring.garderie.entity.ConditionSanitaire;

import java.util.List;

public interface ConditionSanitaireService {

    ConditionSanitaire addCondition(Long enfantId, ConditionSanitaire condition);

    List<ConditionSanitaire> getConditionsByEnfant(Long enfantId);

    ConditionSanitaire updateCondition(Long id, ConditionSanitaire updatedCondition);

    void deleteCondition(Long id);
}

