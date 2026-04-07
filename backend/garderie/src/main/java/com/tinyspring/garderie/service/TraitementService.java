package com.tinyspring.garderie.service;

import com.tinyspring.garderie.entity.Traitement;
import com.tinyspring.garderie.entity.ConditionSanitaire;
import com.tinyspring.garderie.repository.TraitementRepository;
import com.tinyspring.garderie.repository.ConditionSanitaireRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TraitementService {

    private final TraitementRepository traitementRepository;
    private final ConditionSanitaireRepository conditionRepository;

    public TraitementService(TraitementRepository traitementRepository,
                             ConditionSanitaireRepository conditionRepository) {
        this.traitementRepository = traitementRepository;
        this.conditionRepository = conditionRepository;
    }

    // Ajouter un traitement à une condition sanitaire
    public Traitement ajouterTraitement(Long conditionId, Traitement traitement) {
        ConditionSanitaire condition = conditionRepository.findById(conditionId)
                .orElseThrow(() -> new RuntimeException("ConditionSanitaire non trouvée"));
        traitement.setConditionSanitaire(condition);
        traitement.setStatut(com.tinyspring.garderie.entity.StatutTraitement.EN_ATTENTE_VALIDATION);
        return traitementRepository.save(traitement);
    }

    // Lister les traitements d'une condition sanitaire
    public List<Traitement> listerTraitementsParCondition(Long conditionId) {
        return traitementRepository.findByConditionSanitaireId(conditionId);
    }

    // Lister tous les traitements d'un enfant via ses conditions
    public List<Traitement> listerTraitementsParEnfant(Long enfantId) {
        return traitementRepository.findByConditionSanitaire_EnfantId(enfantId);
    }

    // Modifier un traitement
    public Traitement modifierTraitement(Long traitementId, Traitement nouveauTraitement) {
        Traitement t = traitementRepository.findById(traitementId)
                .orElseThrow(() -> new RuntimeException("Traitement non trouvé"));
        t.setNomTraitement(nouveauTraitement.getNomTraitement());
        t.setDescription(nouveauTraitement.getDescription());
        t.setOrdonnance(nouveauTraitement.getOrdonnance());
        t.setDateDebut(nouveauTraitement.getDateDebut());
        t.setDateFin(nouveauTraitement.getDateFin());
        t.setHeuresPrises(nouveauTraitement.getHeuresPrises());
        t.setStatut(nouveauTraitement.getStatut());
        return traitementRepository.save(t);
    }

    // Supprimer un traitement
    public void supprimerTraitement(Long traitementId) {
        traitementRepository.deleteById(traitementId);
    }
}