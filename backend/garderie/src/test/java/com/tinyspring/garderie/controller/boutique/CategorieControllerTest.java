package com.tinyspring.garderie.controller.boutique;

import com.tinyspring.garderie.dto.boutique.CategorieDto;
import com.tinyspring.garderie.service.boutique.CategorieService;
import com.tinyspring.garderie.service.boutique.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests CategorieController")
class CategorieControllerTest {

    @Mock private CategorieService categorieService;
    @Mock private FileStorageService fileStorageService;

    @InjectMocks
    private CategorieController categorieController;

    private CategorieDto categorieDto;

    @BeforeEach
    void setUp() {
        categorieDto = CategorieDto.builder()
                .id(1L).nom("Livres enfants")
                .description("Livres pour enfants")
                .imageUrl("/images/livres.jpg")
                .nombreProduits(5)
                .build();
    }

    @Test
    @DisplayName("getAllPublic() retourne 200 avec liste catégories")
    void getAllPublic_shouldReturn200() {
        when(categorieService.findAll()).thenReturn(List.of(categorieDto));

        ResponseEntity<List<CategorieDto>> response =
                categorieController.getAllPublic();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getNom()).isEqualTo("Livres enfants");
    }

    @Test
    @DisplayName("getByIdPublic() retourne 200 avec la catégorie")
    void getByIdPublic_shouldReturn200() {
        when(categorieService.findById(1L)).thenReturn(categorieDto);

        ResponseEntity<CategorieDto> response =
                categorieController.getByIdPublic(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getAllAdmin() retourne 200 avec liste catégories")
    void getAllAdmin_shouldReturn200() {
        when(categorieService.findAll()).thenReturn(List.of(categorieDto));

        ResponseEntity<List<CategorieDto>> response =
                categorieController.getAllAdmin();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    @DisplayName("create() sans image retourne 201")
    void create_withoutImage_shouldReturn201() {
        when(categorieService.create(any())).thenReturn(categorieDto);

        ResponseEntity<CategorieDto> response = categorieController.create(
                "Livres enfants", "Description", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getNom()).isEqualTo("Livres enfants");
    }

    @Test
    @DisplayName("delete() retourne 409 si catégorie a des produits")
    void delete_shouldReturn409_whenHasProduits() {
        when(categorieService.findById(1L)).thenReturn(categorieDto);
        // nombreProduits = 5 → conflit

        ResponseEntity<?> response = categorieController.delete(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(categorieService, never()).delete(any());
    }

    @Test
    @DisplayName("delete() retourne 204 si catégorie sans produits")
    void delete_shouldReturn204_whenNoProduits() {
        CategorieDto vide = CategorieDto.builder()
                .id(2L).nom("Vide").nombreProduits(0)
                .imageUrl(null).build();
        when(categorieService.findById(2L)).thenReturn(vide);

        ResponseEntity<?> response = categorieController.delete(2L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(categorieService).delete(2L);
    }
}