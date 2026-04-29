package com.tinyspring.garderie.service.RH;

import com.tinyspring.garderie.dto.RH.AbsenceCongeDTO;
import com.tinyspring.garderie.entity.RH.enums.StatutAbsenceConge;

import java.util.List;

public interface IAbsenceCongeService {

    // ===== ADMIN =====
    List<AbsenceCongeDTO> getAllAbsenceConges();

    // ✅ AJOUT
    AbsenceCongeDTO getAbsenceCongeById(Long id);

    List<AbsenceCongeDTO> getAbsenceCongesByStatut(StatutAbsenceConge statut);

    AbsenceCongeDTO validerDemande(Long id);

    AbsenceCongeDTO refuserDemande(Long id);

    void deleteAbsenceConge(Long id);

    // ===== ANIMATRICE =====
    AbsenceCongeDTO soumettreDemandeAbsenceConge(AbsenceCongeDTO dto);

    List<AbsenceCongeDTO> getMesAbsenceConges(Long animatriceId);
}