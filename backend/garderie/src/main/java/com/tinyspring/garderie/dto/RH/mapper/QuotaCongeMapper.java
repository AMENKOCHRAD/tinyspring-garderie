package com.tinyspring.garderie.dto.RH.mapper;

import com.tinyspring.garderie.dto.RH.QuotaCongeDTO;
import com.tinyspring.garderie.entity.RH.QuotaConge;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface QuotaCongeMapper {

    @Mapping(target = "joursUtilisesAnneeEnCours", ignore = true)
    @Mapping(target = "joursRestants", ignore = true)
    QuotaCongeDTO toDTO(QuotaConge quota);

    @Mapping(target = "id", ignore = true)
    QuotaConge toEntity(QuotaCongeDTO dto);
}