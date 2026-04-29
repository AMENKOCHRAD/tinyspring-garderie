package com.tinyspring.garderie.service;

import com.tinyspring.garderie.dto.EnfantDTO;
import com.tinyspring.garderie.dto.EnfantResponseDTO;
import com.tinyspring.garderie.entity.Enfant;

import java.util.List;
import java.util.Optional;

public interface EnfantService {

    Enfant ajouterEnfant(EnfantDTO dto);

    List<Enfant> getEnfantsParParent(Long parentId);

    Optional<Enfant> getEnfantParId(Long id);

    Enfant modifierEnfant(Long id, EnfantDTO dto);

    Enfant archiverEnfant(Long id);

    List<EnfantResponseDTO> getAllEnfants();

    EnfantResponseDTO getEnfantDTOById(Long id);
}

