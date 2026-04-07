package com.tinyspring.garderie.service;

import com.tinyspring.garderie.entity.ConditionSanitaire;
import com.tinyspring.garderie.entity.Enfant;
import com.tinyspring.garderie.repository.ConditionSanitaireRepository;
import com.tinyspring.garderie.repository.EnfantRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConditionSanitaireService {

    private final ConditionSanitaireRepository conditionRepo;
    private final EnfantRepository enfantRepo;

    public ConditionSanitaireService(ConditionSanitaireRepository conditionRepo, EnfantRepository enfantRepo) {
        this.conditionRepo = conditionRepo;
        this.enfantRepo = enfantRepo;
    }

    public ConditionSanitaire addCondition(Long enfantId, ConditionSanitaire condition) {
        Enfant enfant = enfantRepo.findById(enfantId)
                .orElseThrow(() -> new RuntimeException("Enfant non trouvé"));
        condition.setEnfant(enfant);
        return conditionRepo.save(condition);
    }

    public List<ConditionSanitaire> getConditionsByEnfant(Long enfantId) {
        return conditionRepo.findByEnfantId(enfantId);
    }

    public ConditionSanitaire updateCondition(Long id, ConditionSanitaire updatedCondition) {
        ConditionSanitaire condition = conditionRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Condition non trouvée"));
        condition.setNomCondition(updatedCondition.getNomCondition());
        condition.setType(updatedCondition.getType());
        condition.setDescription(updatedCondition.getDescription());
        condition.setDateDebut(updatedCondition.getDateDebut());
        condition.setDateFin(updatedCondition.getDateFin());
        return conditionRepo.save(condition);
    }

    public void deleteCondition(Long id) {
        conditionRepo.deleteById(id);
    }
}