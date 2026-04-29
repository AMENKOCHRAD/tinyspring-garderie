package com.tinyspring.garderie.service;

import com.tinyspring.garderie.dto.TraitementCreateDto;
import com.tinyspring.garderie.dto.TraitementUpdateDto;
import com.tinyspring.garderie.dto.TraitementValidationDto;
import com.tinyspring.garderie.entity.ConditionSanitaire;
import com.tinyspring.garderie.entity.Enfant;
import com.tinyspring.garderie.entity.StatutTraitement;
import com.tinyspring.garderie.entity.Traitement;
import com.tinyspring.garderie.mapper.TraitementMapper;
import com.tinyspring.garderie.repository.ConditionSanitaireRepository;
import com.tinyspring.garderie.repository.TraitementRepository;
import com.tinyspring.garderie.repository.UserRepository;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TraitementServiceImpl implements TraitementService {

    private final TraitementRepository traitementRepository;
    private final ConditionSanitaireRepository conditionSanitaireRepository;
    private final UserRepository userRepository;
    private final OrdonnanceStorageService ordonnanceStorageService;
    private final TraitementAutoValidationService autoValidationService;
    private final RealtimeNotificationService realtimeNotificationService;
    private final TraitementValidationHistoryService validationHistoryService;
    private final TraitementMapper traitementMapper;

    public TraitementServiceImpl(TraitementRepository traitementRepository,
                              ConditionSanitaireRepository conditionSanitaireRepository,
                              UserRepository userRepository,
                              OrdonnanceStorageService ordonnanceStorageService,
                              TraitementAutoValidationService autoValidationService,
                              RealtimeNotificationService realtimeNotificationService,
                              TraitementValidationHistoryService validationHistoryService,
                              TraitementMapper traitementMapper) {
        this.traitementRepository = traitementRepository;
        this.conditionSanitaireRepository = conditionSanitaireRepository;
        this.userRepository = userRepository;
        this.ordonnanceStorageService = ordonnanceStorageService;
        this.autoValidationService = autoValidationService;
        this.realtimeNotificationService = realtimeNotificationService;
        this.validationHistoryService = validationHistoryService;
        this.traitementMapper = traitementMapper;
    }

    public Traitement ajouterTraitement(Long conditionId, Traitement traitement) {
        ConditionSanitaire condition = conditionSanitaireRepository.findById(conditionId)
                .orElseThrow(() -> new RuntimeException("Condition sanitaire introuvable"));

        validateNewDates(traitement.getDateDebut(), traitement.getDateFin());

        traitement.setConditionSanitaire(condition);

        TraitementAutoValidationService.Result res = autoValidationService.evaluer(traitement);
        applyDecision(traitement, res);

        Traitement saved = traitementRepository.save(traitement);
        validationHistoryService.recordSystemDecision(saved, res);
        notifyParentIfNeeded(saved, res);
        return saved;
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

        validateNewDates(payload.getDateDebut(), payload.getDateFin());

        Traitement traitement = traitementMapper.fromCreateDto(payload);
        traitement.setConditionSanitaire(condition);
        traitement.setOrdonnance(storedPdfFilename);

        TraitementAutoValidationService.Result res = autoValidationService.evaluer(traitement);
        applyDecision(traitement, res);

        Traitement saved = traitementRepository.save(traitement);
        validationHistoryService.recordSystemDecision(saved, res);
        notifyParentIfNeeded(saved, res);
        return saved;
    }

    private void validateNewDates(LocalDate dateDebut, LocalDate dateFin) {
        if (dateDebut == null) {
            throw new RuntimeException("La date de debut est obligatoire");
        }

        LocalDate today = LocalDate.now();
        if (dateDebut.isBefore(today)) {
            throw new RuntimeException("La date de debut ne peut pas etre dans le passe");
        }

        if (dateFin != null && dateFin.isBefore(dateDebut)) {
            throw new RuntimeException("La date de fin doit etre apres la date de debut");
        }
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

        traitementMapper.updateEntityFromUpdateDto(payload, traitement);

        validateNewDates(payload.getDateDebut(), payload.getDateFin());

        TraitementAutoValidationService.Result res = autoValidationService.evaluer(traitement);
        applyDecision(traitement, res);

        Traitement saved = traitementRepository.save(traitement);
        validationHistoryService.recordSystemDecision(saved, res);
        notifyParentIfNeeded(saved, res);
        return saved;
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
        Traitement saved = traitementRepository.save(traitement);
        validationHistoryService.recordAdminDecision(saved, null, "VALIDE_ADMIN", "Traitement valide par l'admin.");
        notifyParentValidation(saved, "VALIDE", "Traitement valide par l'admin.");
        return saved;
    }

    public Traitement refuserTraitementAdmin(Long traitementId, String adminEmail, String note) {
        Traitement traitement = traitementRepository.findById(traitementId)
                .orElseThrow(() -> new RuntimeException("Traitement introuvable"));
        traitement.setStatut(StatutTraitement.REFUSE);
        Traitement saved = traitementRepository.save(traitement);
        validationHistoryService.recordAdminDecision(saved, adminEmail, "REFUSE_ADMIN", note != null ? note : "Traitement refuse par l'admin.");
        notifyParentValidation(saved, "REFUSE", note != null ? note : "Traitement refuse par l'admin.");
        return saved;
    }

    private void applyDecision(Traitement traitement, TraitementAutoValidationService.Result res) {
        if (res == null) {
            traitement.setStatut(StatutTraitement.EN_ATTENTE_VALIDATION);
            traitement.setAutoValidationNote(null);
            return;
        }

        traitement.setAutoValidationNote(res.note);
        if (res.decision == TraitementAutoValidationService.Decision.ACCEPTE) {
            traitement.setStatut(StatutTraitement.VALIDE);
        } else if (res.decision == TraitementAutoValidationService.Decision.REFUSE) {
            traitement.setStatut(StatutTraitement.REFUSE);
        } else {
            traitement.setStatut(StatutTraitement.EN_ATTENTE_VALIDATION);
        }
    }

    private void notifyParentIfNeeded(Traitement traitement, TraitementAutoValidationService.Result res) {
        if (traitement == null || res == null) {
            return;
        }
        if (res.decision == TraitementAutoValidationService.Decision.A_VERIFIER) {
            return;
        }

        String statut = traitement.getStatut() != null ? traitement.getStatut().name() : null;
        notifyParentValidation(traitement, statut, res.note);
    }

    private void notifyParentValidation(Traitement traitement, String statut, String note) {
        ConditionSanitaire condition = traitement.getConditionSanitaire();
        Enfant enfant = condition != null ? condition.getEnfant() : null;
        String parentEmail = enfant != null && enfant.getParent() != null ? enfant.getParent().getEmail() : null;

        realtimeNotificationService.notifyParentTraitementValidation(
                parentEmail,
                enfant != null ? enfant.getId() : null,
                traitement.getId(),
                traitement.getNomTraitement(),
                statut,
                note
        );
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
        return traitementMapper.toValidationDto(traitement);
    }
}
