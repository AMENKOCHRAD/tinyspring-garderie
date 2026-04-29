package com.tinyspring.garderie.mapper.boutique;

import com.tinyspring.garderie.dto.boutique.CategorieDto;
import com.tinyspring.garderie.entity.boutique.Categorie;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CategorieMapper {

    @Mapping(
            target = "nombreProduits",
            expression = "java(categorie.getProduits() != null ? categorie.getProduits().size() : 0)"
    )
    CategorieDto toDto(Categorie categorie);

    @Mapping(target = "produits", ignore = true)
    @Mapping(target = "id", ignore = true)
    Categorie toEntity(CategorieDto dto);
}