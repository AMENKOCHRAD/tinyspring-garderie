package com.tinyspring.garderie.mapper.boutique;

import com.tinyspring.garderie.dto.boutique.CategorieDto;
import com.tinyspring.garderie.entity.boutique.Categorie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Tests CategorieMapper")
class CategorieMapperTest {

    private final CategorieMapper mapper =
            Mappers.getMapper(CategorieMapper.class);

    @Test
    @DisplayName("toDto() mappe correctement Categorie → CategorieDto")
    void toDto_shouldMapCorrectly() {
        Categorie categorie = Categorie.builder()
                .id(1L).nom("Livres enfants")
                .description("Livres pour enfants")
                .imageUrl("/images/livres.jpg")
                .produits(List.of())
                .build();

        CategorieDto result = mapper.toDto(categorie);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getNom()).isEqualTo("Livres enfants");
        assertThat(result.getDescription()).isEqualTo("Livres pour enfants");
        assertThat(result.getImageUrl()).isEqualTo("/images/livres.jpg");
        assertThat(result.getNombreProduits()).isEqualTo(0);
    }

    @Test
    @DisplayName("toDto() calcule nombreProduits correctement")
    void toDto_shouldCalculateNombreProduits() {
        Categorie categorie = Categorie.builder()
                .id(1L).nom("Test")
                .produits(null)
                .build();

        CategorieDto result = mapper.toDto(categorie);

        assertThat(result.getNombreProduits()).isEqualTo(0);
    }

    @Test
    @DisplayName("toEntity() mappe correctement CategorieDto → Categorie")
    void toEntity_shouldMapCorrectly() {
        CategorieDto dto = CategorieDto.builder()
                .nom("Livres enfants")
                .description("Livres pour enfants")
                .imageUrl("/images/livres.jpg")
                .build();

        Categorie result = mapper.toEntity(dto);

        assertThat(result.getNom()).isEqualTo("Livres enfants");
        assertThat(result.getDescription()).isEqualTo("Livres pour enfants");
        assertThat(result.getImageUrl()).isEqualTo("/images/livres.jpg");
        assertThat(result.getId()).isNull();
    }
}