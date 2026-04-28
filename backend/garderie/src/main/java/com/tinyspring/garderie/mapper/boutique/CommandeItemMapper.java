package com.tinyspring.garderie.mapper.boutique;

import com.tinyspring.garderie.dto.boutique.CommandeItemDto;
import com.tinyspring.garderie.entity.boutique.CommandeProduit;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CommandeItemMapper {

    @Mapping(source = "produit.id",       target = "produitId")
    @Mapping(source = "produit.nom",      target = "produitNom")
    @Mapping(source = "produit.imageUrl", target = "produitImageUrl")
    @Mapping(target = "sousTotal",
            expression = "java(item.getPrixUnitaire() * item.getQuantite())")
    CommandeItemDto toDto(CommandeProduit item);
}