package com.tinyspring.garderie.dto.RH.mapper;

import com.tinyspring.garderie.dto.RH.AnimatriceDTO;
import com.tinyspring.garderie.entity.RH.Animatrice;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AnimatriceMapper {

    AnimatriceDTO toDTO(Animatrice animatrice);

    @Mapping(target = "absenceConges", ignore = true)
    @Mapping(target = "motDePasseTemporaire", ignore = true)
    Animatrice toEntity(AnimatriceDTO dto);
}