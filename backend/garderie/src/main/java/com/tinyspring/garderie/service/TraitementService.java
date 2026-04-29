package com.tinyspring.garderie.service;

import com.tinyspring.garderie.dto.TraitementCreateDto;
import com.tinyspring.garderie.dto.TraitementUpdateDto;
import com.tinyspring.garderie.dto.TraitementValidationDto;
import com.tinyspring.garderie.entity.Traitement;
import org.springframework.core.io.Resource;

import java.util.List;

public interface TraitementService {

    Traitement ajouterTraitement(Long conditionId, Traitement traitement);

    List<Traitement> listerTraitementsParCondition(Long conditionId);

    List<Traitement> listerTraitementsParEnfant(Long enfantId);

    Traitement modifierTraitement(Long traitementId, Traitement updatedTraitement);

    Traitement ajouterTraitementAvecOrdonnance(Long conditionId, TraitementCreateDto payload, String storedPdfFilename);

    Traitement modifierTraitementParParent(String emailParent, Long traitementId, TraitementUpdateDto payload);

    String enregistrerOrdonnancePdf(org.springframework.web.multipart.MultipartFile ordonnancePdf);

    Traitement annulerTraitementParParent(String emailParent, Long traitementId);

    void supprimerTraitementParParent(String emailParent, Long traitementId);

    Resource chargerOrdonnanceResourceParParent(String emailParent, Long traitementId);

    Resource chargerOrdonnanceResourceAdmin(Long traitementId);

    void supprimerTraitement(Long traitementId);

    List<TraitementValidationDto> listerTraitementsEnAttenteValidation();

    TraitementValidationDto consulterTraitement(Long traitementId);

    Traitement validerTraitement(Long traitementId);

    Traitement refuserTraitementAdmin(Long traitementId, String emailAdmin, String note);
}

