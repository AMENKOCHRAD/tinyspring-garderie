package com.tinyspring.garderie.dto.RH.mapper;

import com.tinyspring.garderie.dto.RH.RapportRHDTO;
import com.tinyspring.garderie.entity.RH.RapportRH;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RapportRHMapper {

    // ✅ Pas besoin de @Mapping — MapStruct ignore automatiquement
    // les champs de l'entité absents du DTO (donneesContexte)
    RapportRHDTO toDTO(RapportRH rapport);
}