package com.tinyspring.garderie.mapper.boutique;

import com.tinyspring.garderie.dto.boutique.ProduitDto;
import com.tinyspring.garderie.dto.boutique.UserCategorieScoreAdminDto;
import com.tinyspring.garderie.entity.boutique.Produit;
import com.tinyspring.garderie.entity.boutique.UserCategorieScore;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AffiniteMapper {

    // Produit → ProduitDto
    @Mapping(source = "categorie.id",  target = "categorieId")
    @Mapping(source = "categorie.nom", target = "categorieNom")
    ProduitDto toProduitDto(Produit produit);

    // UserCategorieScore → UserCategorieScoreAdminDto
    @Mapping(source = "user.id",       target = "userId")
    @Mapping(source = "user.nom",      target = "userNom")
    @Mapping(source = "user.email",    target = "userEmail")
    @Mapping(source = "categorie.id",  target = "categorieId")
    @Mapping(source = "categorie.nom", target = "categorieNom")
    UserCategorieScoreAdminDto toAdminDto(UserCategorieScore score);
}