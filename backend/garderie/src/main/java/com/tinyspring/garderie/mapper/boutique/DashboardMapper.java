package com.tinyspring.garderie.mapper.boutique;

import com.tinyspring.garderie.dto.boutique.DashboardRecentCommandeDto;
import com.tinyspring.garderie.entity.boutique.Commande;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DashboardMapper {

    @Mapping(source = "user.nom",   target = "userNom")
    @Mapping(source = "user.email", target = "userEmail")
    DashboardRecentCommandeDto toRecentCommandeDto(Commande commande);
}