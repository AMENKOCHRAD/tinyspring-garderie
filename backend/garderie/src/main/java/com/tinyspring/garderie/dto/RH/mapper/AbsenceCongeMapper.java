package com.tinyspring.garderie.dto.RH.mapper;

import com.tinyspring.garderie.dto.RH.AbsenceCongeDTO;
import com.tinyspring.garderie.entity.RH.AbsenceConge;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AbsenceCongeMapper {

    @Mapping(source = "animatrice.id",     target = "animatriceId")
    @Mapping(source = "animatrice.nom",    target = "animatriceNom")
    @Mapping(source = "animatrice.prenom", target = "animatricePrenom")
    @Mapping(target = "resultatEvaluation", ignore = true)
    AbsenceCongeDTO toDTO(AbsenceConge absenceConge);
}