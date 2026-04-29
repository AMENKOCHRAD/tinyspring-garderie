package com.tinyspring.garderie.mapper;

import com.tinyspring.garderie.dto.TraitementCreateDto;
import com.tinyspring.garderie.dto.TraitementUpdateDto;
import com.tinyspring.garderie.dto.TraitementValidationDto;
import com.tinyspring.garderie.entity.StatutTraitement;
import com.tinyspring.garderie.entity.Traitement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface TraitementMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "conditionSanitaire", ignore = true)
    @Mapping(target = "autoValidationNote", ignore = true)
    @Mapping(target = "statut", ignore = true)
    Traitement fromCreateDto(TraitementCreateDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "conditionSanitaire", ignore = true)
    @Mapping(target = "autoValidationNote", ignore = true)
    @Mapping(target = "statut", ignore = true)
    void updateEntityFromUpdateDto(TraitementUpdateDto dto, @MappingTarget Traitement entity);

    @Mapping(target = "traitementId", source = "id")
    @Mapping(target = "statut", source = "statut", qualifiedByName = "statutToString")
    @Mapping(target = "conditionId", source = "conditionSanitaire.id")
    @Mapping(target = "nomCondition", source = "conditionSanitaire.nomCondition")
    @Mapping(target = "typeCondition", source = "conditionSanitaire.type", qualifiedByName = "enumToName")
    @Mapping(target = "descriptionCondition", source = "conditionSanitaire.description")
    @Mapping(target = "enfantId", source = "conditionSanitaire.enfant.id")
    @Mapping(target = "nomEnfant", source = "conditionSanitaire.enfant.nom")
    @Mapping(target = "prenomEnfant", source = "conditionSanitaire.enfant.prenom")
    @Mapping(target = "nomParent", source = "conditionSanitaire.enfant.parent.nom")
    @Mapping(target = "emailParent", source = "conditionSanitaire.enfant.parent.email")
    TraitementValidationDto toValidationDto(Traitement traitement);

    @Named("statutToString")
    default String statutToString(StatutTraitement statut) {
        return statut != null ? statut.name() : null;
    }

    @Named("enumToName")
    default String enumToName(Enum<?> value) {
        return value != null ? value.name() : null;
    }
}

