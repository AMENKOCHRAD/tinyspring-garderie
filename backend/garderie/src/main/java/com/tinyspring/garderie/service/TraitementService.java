package com.tinyspring.garderie.service;

import com.tinyspring.garderie.dto.TraitementValidationDto;
import com.tinyspring.garderie.entity.ConditionSanitaire;
import com.tinyspring.garderie.entity.Enfant;
import com.tinyspring.garderie.entity.StatutTraitement;
import com.tinyspring.garderie.entity.Traitement;
import com.tinyspring.garderie.repository.ConditionSanitaireRepository;
import com.tinyspring.garderie.repository.TraitementRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TraitementService {

    private final TraitementRepository traitementRepository;
    private final ConditionSanitaireRepository conditionSanitaireRepository;

    public TraitementService(TraitementRepository traitementRepository,
                             ConditionSanitaireRepository conditionSanitaireRepository) {
        this.traitementRepository = traitementRepository;
        this.conditionSanitaireRepository = conditionSanitaireRepository;
    }

    public Traitement ajouterTraitement(Long conditionId, Traitement traitement) {
        ConditionSanitaire condition = conditionSanitaireRepository.findById(conditionId)
                .orElseThrow(() -> new RuntimeException("Condition sanitaire introuvable"));

        traitement.setConditionSanitaire(condition);
        traitement.setStatut(StatutTraitement.EN_ATTENTE_VALIDATION);

        return traitementRepository.save(traitement);
    }

    public List<Traitement> listerTraitementsParCondition(Long conditionId) {
        return traitementRepository.findByConditionSanitaireId(conditionId);
    }

    public List<Traitement> listerTraitementsParEnfant(Long enfantId) {
        return traitementRepository.findByConditionSanitaireEnfantId(enfantId);
    }

    public Traitement modifierTraitement(Long traitementId, Traitement updatedTraitement) {
        Traitement traitement = traitementRepository.findById(traitementId)
                .orElseThrow(() -> new RuntimeException("Traitement introuvable"));

        traitement.setNomTraitement(updatedTraitement.getNomTraitement());
        traitement.setDescription(updatedTraitement.getDescription());
        traitement.setOrdonnance(updatedTraitement.getOrdonnance());
        traitement.setDateDebut(updatedTraitement.getDateDebut());
        traitement.setDateFin(updatedTraitement.getDateFin());
        traitement.setHeuresPrises(updatedTraitement.getHeuresPrises());
        traitement.setStatut(updatedTraitement.getStatut());

        return traitementRepository.save(traitement);
    }

    public void supprimerTraitement(Long traitementId) {
        traitementRepository.deleteById(traitementId);
    }

    // ✅ récupérer tous les traitements EN_ATTENTE_VALIDATION
    public List<TraitementValidationDto> listerTraitementsEnAttenteValidation() {
        List<Traitement> traitements = traitementRepository.findByStatut(StatutTraitement.EN_ATTENTE_VALIDATION);

        return traitements.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    // ✅ consulter les détails d'un traitement
    public TraitementValidationDto consulterTraitement(Long traitementId) {
        Traitement traitement = traitementRepository.findById(traitementId)
                .orElseThrow(() -> new RuntimeException("Traitement introuvable"));

        return mapToDto(traitement);
    }

    // ✅ valider un traitement
    public Traitement validerTraitement(Long traitementId) {
        Traitement traitement = traitementRepository.findById(traitementId)
                .orElseThrow(() -> new RuntimeException("Traitement introuvable"));

        traitement.setStatut(StatutTraitement.VALIDE);
        return traitementRepository.save(traitement);
    }

    private TraitementValidationDto mapToDto(Traitement traitement) {
        TraitementValidationDto dto = new TraitementValidationDto();

        dto.setTraitementId(traitement.getId());
        dto.setNomTraitement(traitement.getNomTraitement());
        dto.setDescription(traitement.getDescription());
        dto.setOrdonnance(traitement.getOrdonnance());
        dto.setDateDebut(traitement.getDateDebut());
        dto.setDateFin(traitement.getDateFin());
        dto.setHeuresPrises(traitement.getHeuresPrises());
        dto.setStatut(traitement.getStatut() != null ? traitement.getStatut().name() : null);

        ConditionSanitaire condition = traitement.getConditionSanitaire();
        if (condition != null) {
            dto.setConditionId(condition.getId());
            dto.setNomCondition(condition.getNomCondition());
            dto.setTypeCondition(condition.getType());
            dto.setDescriptionCondition(condition.getDescription());

            Enfant enfant = condition.getEnfant();
            if (enfant != null) {
                dto.setEnfantId(enfant.getId());
                dto.setNomEnfant(enfant.getNom());
                dto.setPrenomEnfant(enfant.getPrenom());

                if (enfant.getParent() != null) {
                    dto.setNomParent(enfant.getParent().getNom());
                    dto.setEmailParent(enfant.getParent().getEmail());
                }
            }
        }

        return dto;
    }
}