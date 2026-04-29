package com.tinyspring.garderie.mapper;

import com.tinyspring.garderie.dto.EnfantDTO;
import com.tinyspring.garderie.dto.EnfantResponseDTO;
import com.tinyspring.garderie.entity.Enfant;
import com.tinyspring.garderie.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.time.LocalDate;

@Mapper(componentModel = "spring")
public interface EnfantMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "archive", ignore = true)
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "dateNaissance", source = "dateNaissance", qualifiedByName = "stringToLocalDate")
    Enfant toEntity(EnfantDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "archive", ignore = true)
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "dateNaissance", source = "dateNaissance", qualifiedByName = "stringToLocalDate")
    void updateEntityFromDto(EnfantDTO dto, @MappingTarget Enfant enfant);

    @Mapping(target = "dateNaissance", source = "dateNaissance", qualifiedByName = "localDateToString")
    @Mapping(target = "parent", source = "parent")
    EnfantResponseDTO toResponseDto(Enfant enfant);

    EnfantResponseDTO.ParentDTO toParentDto(User user);

    @Named("stringToLocalDate")
    default LocalDate stringToLocalDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return LocalDate.parse(value.trim());
    }

    @Named("localDateToString")
    default String localDateToString(LocalDate value) {
        return value != null ? value.toString() : null;
    }
}

