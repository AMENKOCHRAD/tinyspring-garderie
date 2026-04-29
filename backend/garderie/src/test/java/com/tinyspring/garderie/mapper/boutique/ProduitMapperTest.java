package com.tinyspring.garderie.mapper.boutique;

import com.tinyspring.garderie.dto.boutique.ProduitDto;
import com.tinyspring.garderie.entity.boutique.Categorie;
import com.tinyspring.garderie.entity.boutique.Produit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Tests ProduitMapper")
class ProduitMapperTest {

    private final ProduitMapper mapper =
            Mappers.getMapper(ProduitMapper.class);

    @Test
    @DisplayName("toDto() mappe correctement Produit → ProduitDto")
    void toDto_shouldMapCorrectly() {
        Categorie categorie = Categorie.builder()
                .id(1L).nom("Livres enfants").build();

        Produit produit = Produit.builder()
                .id(9L).nom("Le Petit Prince")
                .description("Livre classique")
                .prix(15.99).stock(10).seuilAlerte(3)
                .imageUrl("/images/livre.jpg")
                .categorie(categorie)
                .build();

        ProduitDto result = mapper.toDto(produit);

        assertThat(result.getId()).isEqualTo(9L);
        assertThat(result.getNom()).isEqualTo("Le Petit Prince");
        assertThat(result.getPrix()).isEqualTo(15.99);
        assertThat(result.getStock()).isEqualTo(10);
        assertThat(result.getCategorieId()).isEqualTo(1L);
        assertThat(result.getCategorieNom()).isEqualTo("Livres enfants");
    }

    @Test
    @DisplayName("toEntity() mappe correctement ProduitDto → Produit")
    void toEntity_shouldMapCorrectly() {
        ProduitDto dto = ProduitDto.builder()
                .nom("Le Petit Prince")
                .description("Livre classique")
                .prix(15.99).stock(10)
                .imageUrl("/images/livre.jpg")
                .categorieId(1L)
                .build();

        Produit result = mapper.toEntity(dto);

        assertThat(result.getNom()).isEqualTo("Le Petit Prince");
        assertThat(result.getPrix()).isEqualTo(15.99);
        assertThat(result.getStock()).isEqualTo(10);
        assertThat(result.getCategorie()).isNull();
        assertThat(result.getId()).isNull();
    }
}