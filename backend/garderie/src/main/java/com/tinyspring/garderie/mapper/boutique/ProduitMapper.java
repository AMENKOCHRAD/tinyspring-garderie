package com.tinyspring.garderie.mapper.boutique;

import com.tinyspring.garderie.dto.boutique.ProduitDto;
import com.tinyspring.garderie.entity.boutique.Produit;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProduitMapper {

    // Produit → ProduitDto
    // categorie.id → categorieId  |  categorie.nom → categorieNom
    @Mapping(source = "categorie.id",  target = "categorieId")
    @Mapping(source = "categorie.nom", target = "categorieNom")
    ProduitDto toDto(Produit produit);

    // ProduitDto → Produit
    // categorie est ignorée ici → le service la charge depuis la BD
    @Mapping(target = "categorie", ignore = true)
    @Mapping(target = "id",        ignore = true)
    Produit toEntity(ProduitDto dto);
}