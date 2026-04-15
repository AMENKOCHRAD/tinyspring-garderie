package com.tinyspring.garderie.service;

import com.tinyspring.garderie.dto.TraitementCreateDto;
import com.tinyspring.garderie.dto.TraitementUpdateDto;
import com.tinyspring.garderie.dto.TraitementValidationDto;
import com.tinyspring.garderie.entity.ConditionSanitaire;
import com.tinyspring.garderie.entity.Enfant;
import com.tinyspring.garderie.entity.StatutTraitement;
import com.tinyspring.garderie.entity.Traitement;
import com.tinyspring.garderie.repository.ConditionSanitaireRepository;
import com.tinyspring.garderie.repository.TraitementRepository;
import com.tinyspring.garderie.repository.UserRepository;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TraitementService {

    private final TraitementRepository traitementRepository;
    private final ConditionSanitaireRepository conditionSanitaireRepository;
    private final UserRepository userRepository;
    private final OrdonnanceStorageService ordonnanceStorageService;

    public TraitementService(TraitementRepository traitementRepository,
                             ConditionSanitaireRepository conditionSanitaireRepository,
                             UserRepository userRepository,
                             OrdonnanceStorageService ordonnanceStorageService) {
        this.traitementRepository = traitementRepository;
        this.conditionSanitaireRepository = conditionSanitaireRepository;
        this.userRepository = userRepository;
        this.ordonnanceStorageService = ordonnanceStorageService;
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

    public Traitement ajouterTraitementAvecOrdonnance(Long conditionId, TraitementCreateDto payload, String storedPdfFilename) {
        ConditionSanitaire condition = conditionSanitaireRepository.findById(conditionId)
                .orElseThrow(() -> new RuntimeException("Condition sanitaire introuvable"));

        Traitement traitement = new Traitement();
        traitement.setConditionSanitaire(condition);
        traitement.setNomTraitement(payload.getNomTraitement());
        traitement.setDescription(payload.getDescription());
        traitement.setDateDebut(payload.getDateDebut());
        traitement.setDateFin(payload.getDateFin());
        traitement.setHeuresPrises(payload.getHeuresPrises());
        traitement.setOrdonnance(storedPdfFilename);
        traitement.setStatut(StatutTraitement.EN_ATTENTE_VALIDATION);

        return traitementRepository.save(traitement);
    }

    public Traitement modifierTraitementParParent(String emailParent, Long traitementId, TraitementUpdateDto payload) {
        if (emailParent == null || emailParent.isBlank()) {
            throw new RuntimeException("Utilisateur non connecte.");
        }

        userRepository.findByEmailIgnoreCase(emailParent.trim())
                .orElseThrow(() -> new RuntimeException("Parent introuvable."));

        Traitement traitement = traitementRepository.findById(traitementId)
                .orElseThrow(() -> new RuntimeException("Traitement introuvable"));

        assertParentOwnsTraitement(emailParent, traitement);

        if (traitement.getStatut() == StatutTraitement.ANNULE) {
            throw new RuntimeException("Impossible de modifier un traitement annule.");
        }

        traitement.setNomTraitement(payload.getNomTraitement());
        traitement.setDescription(payload.getDescription());
        traitement.setOrdonnance(payload.getOrdonnance());
        traitement.setDateDebut(payload.getDateDebut());
        traitement.setDateFin(payload.getDateFin());
        traitement.setHeuresPrises(payload.getHeuresPrises());

        // Toute modification parent -> repasse en attente de validation
        traitement.setStatut(StatutTraitement.EN_ATTENTE_VALIDATION);

        return traitementRepository.save(traitement);
    }

    public String enregistrerOrdonnancePdf(org.springframework.web.multipart.MultipartFile ordonnancePdf) {
        return ordonnanceStorageService.storePdf(ordonnancePdf);
    }

    public Traitement annulerTraitementParParent(String emailParent, Long traitementId) {
        if (emailParent == null || emailParent.isBlank()) {
            throw new RuntimeException("Utilisateur non connecte.");
        }

        userRepository.findByEmailIgnoreCase(emailParent.trim())
                .orElseThrow(() -> new RuntimeException("Parent introuvable."));

        Traitement traitement = traitementRepository.findById(traitementId)
                .orElseThrow(() -> new RuntimeException("Traitement introuvable"));

        assertParentOwnsTraitement(emailParent, traitement);

        traitement.setStatut(StatutTraitement.ANNULE);
        return traitementRepository.save(traitement);
    }

    public void supprimerTraitementParParent(String emailParent, Long traitementId) {
        if (emailParent == null || emailParent.isBlank()) {
            throw new RuntimeException("Utilisateur non connecte.");
        }

        userRepository.findByEmailIgnoreCase(emailParent.trim())
                .orElseThrow(() -> new RuntimeException("Parent introuvable."));

        Traitement traitement = traitementRepository.findById(traitementId)
                .orElseThrow(() -> new RuntimeException("Traitement introuvable"));

        assertParentOwnsTraitement(emailParent, traitement);

        // Garde-fou: si deja actif/valide, on ne supprime pas physiquement
        if (traitement.getStatut() == StatutTraitement.ACTIF || traitement.getStatut() == StatutTraitement.VALIDE) {
            throw new RuntimeException("Impossible de supprimer un traitement actif/valide. Utilisez Annuler.");
        }

        traitementRepository.delete(traitement);
    }

    public Resource chargerOrdonnanceResourceParParent(String emailParent, Long traitementId) {
        if (emailParent == null || emailParent.isBlank()) {
            throw new RuntimeException("Utilisateur non connecte.");
        }

        Traitement traitement = traitementRepository.findById(traitementId)
                .orElseThrow(() -> new RuntimeException("Traitement introuvable"));

        assertParentOwnsTraitement(emailParent, traitement);
        return ordonnanceStorageService.loadAsResource(traitement.getOrdonnance());
    }

    public Resource chargerOrdonnanceResourceAdmin(Long traitementId) {
        Traitement traitement = traitementRepository.findById(traitementId)
                .orElseThrow(() -> new RuntimeException("Traitement introuvable"));

        return ordonnanceStorageService.loadAsResource(traitement.getOrdonnance());
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

    private void assertParentOwnsTraitement(String emailParent, Traitement traitement) {
        ConditionSanitaire condition = traitement.getConditionSanitaire();
        Enfant enfant = condition != null ? condition.getEnfant() : null;

        if (enfant == null || enfant.getParent() == null || enfant.getParent().getEmail() == null) {
            throw new RuntimeException("Impossible de verifier les droits du parent.");
        }

        if (!enfant.getParent().getEmail().equalsIgnoreCase(emailParent)) {
            throw new RuntimeException("Acces interdit.");
        }
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
            dto.setTypeCondition(condition.getType().name());
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
