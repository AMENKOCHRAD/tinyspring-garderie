package com.tinyspring.garderie.mapper.boutique;

import com.tinyspring.garderie.dto.boutique.CommandeDto;
import com.tinyspring.garderie.entity.boutique.Commande;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {CommandeItemMapper.class})
public interface CommandeMapper {

    @Mapping(source = "user.id",    target = "userId")
    @Mapping(source = "user.nom",   target = "userNom")
    @Mapping(source = "user.email", target = "userEmail")
    @Mapping(source = "items",      target = "items") // délégué à CommandeItemMapper
    CommandeDto toDto(Commande commande);

    // toEntity pas nécessaire : la création de commande est trop complexe
    // (stock vérif, items calculés, user lookup) → reste entièrement dans le service
}