package com.tinyspring.garderie.service;

import com.tinyspring.garderie.entity.ConditionSanitaire;
import com.tinyspring.garderie.entity.Enfant;
import com.tinyspring.garderie.entity.TypeConditionSanitaire;
import com.tinyspring.garderie.repository.ConditionSanitaireRepository;
import com.tinyspring.garderie.repository.EnfantRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ConditionSanitaireServiceImpl implements ConditionSanitaireService {

    private final ConditionSanitaireRepository conditionRepo;
    private final EnfantRepository enfantRepo;

    public ConditionSanitaireServiceImpl(ConditionSanitaireRepository conditionRepo, EnfantRepository enfantRepo) {
        this.conditionRepo = conditionRepo;
        this.enfantRepo = enfantRepo;
    }

    @Override
    public ConditionSanitaire addCondition(Long enfantId, ConditionSanitaire condition) {
        Enfant enfant = enfantRepo.findById(enfantId)
                .orElseThrow(() -> new RuntimeException("Enfant non trouve"));

        if (condition.getType() == null) {
            throw new RuntimeException("Le type de condition est obligatoire");
        }

        if (condition.getNomCondition() == null || condition.getNomCondition().isBlank()) {
            throw new RuntimeException("Le nom de la condition est obligatoire");
        }

        if (condition.getDateDebut() == null) {
            throw new RuntimeException("La date de debut est obligatoire");
        }

        LocalDate today = LocalDate.now();
        if (condition.getDateDebut().isBefore(today)) {
            throw new RuntimeException("La date de debut ne peut pas etre dans le passe");
        }

        if (condition.getType() == TypeConditionSanitaire.MALADIE_TEMPORAIRE
                && condition.getDateFin() == null) {
            throw new RuntimeException("La date de fin est obligatoire pour une maladie temporaire");
        }

        if (condition.getDateFin() != null && condition.getDateFin().isBefore(condition.getDateDebut())) {
            throw new RuntimeException("La date de fin doit etre apres la date de debut");
        }

        if (condition.getType() == TypeConditionSanitaire.MALADIE_CHRONIQUE) {
            condition.setDateFin(null);
        }

        condition.setEnfant(enfant);
        return conditionRepo.save(condition);
    }

    @Override
    public List<ConditionSanitaire> getConditionsByEnfant(Long enfantId) {
        return conditionRepo.findByEnfantId(enfantId);
    }

    @Override
    public ConditionSanitaire updateCondition(Long id, ConditionSanitaire updatedCondition) {
        ConditionSanitaire condition = conditionRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Condition non trouvee"));

        if (updatedCondition.getType() == null) {
            throw new RuntimeException("Le type de condition est obligatoire");
        }

        if (updatedCondition.getNomCondition() == null || updatedCondition.getNomCondition().isBlank()) {
            throw new RuntimeException("Le nom de la condition est obligatoire");
        }

        if (updatedCondition.getDateDebut() == null) {
            throw new RuntimeException("La date de debut est obligatoire");
        }

        if (updatedCondition.getType() == TypeConditionSanitaire.MALADIE_TEMPORAIRE
                && updatedCondition.getDateFin() == null) {
            throw new RuntimeException("La date de fin est obligatoire pour une maladie temporaire");
        }

        if (updatedCondition.getDateFin() != null && updatedCondition.getDateFin().isBefore(updatedCondition.getDateDebut())) {
            throw new RuntimeException("La date de fin doit etre apres la date de debut");
        }

        condition.setNomCondition(updatedCondition.getNomCondition());
        condition.setType(updatedCondition.getType());
        condition.setDescription(updatedCondition.getDescription());
        condition.setDateDebut(updatedCondition.getDateDebut());
        condition.setDateFin(
                updatedCondition.getType() == TypeConditionSanitaire.MALADIE_CHRONIQUE
                        ? null
                        : updatedCondition.getDateFin()
        );

        return conditionRepo.save(condition);
    }

    @Override
    public void deleteCondition(Long id) {
        conditionRepo.deleteById(id);
    }
}

