package com.tinyspring.garderie.controller.boutique;

import com.tinyspring.garderie.dto.boutique.ProduitDto;
import com.tinyspring.garderie.service.boutique.FileStorageService;
import com.tinyspring.garderie.service.boutique.ProduitService;
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
@DisplayName("Tests ProduitController")
class ProduitControllerTest {

    @Mock private ProduitService produitService;
    @Mock private FileStorageService fileStorageService;

    @InjectMocks
    private ProduitController produitController;

    private ProduitDto produitDto;

    @BeforeEach
    void setUp() {
        produitDto = ProduitDto.builder()
                .id(9L).nom("Le Petit Prince")
                .prix(15.99).stock(10)
                .categorieId(1L).categorieNom("Livres enfants")
                .imageUrl("/images/livre.jpg")
                .build();
    }

    @Test
    @DisplayName("getAllPublic() retourne 200 avec liste produits")
    void getAllPublic_shouldReturn200() {
        ResponseEntity<?> response =
                produitController.getAllPublic(0, 9, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("getAllPublic() avec recherche par nom")
    void getAllPublic_withNom_shouldReturn200() {
        ResponseEntity<?> response =
                produitController.getAllPublic(0, 9, "Prince", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("getAllPublic() avec filtre catégorie")
    void getAllPublic_withCategorie_shouldReturn200() {
        ResponseEntity<?> response =
                produitController.getAllPublic(0, 9, null, 1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
    @Test
    @DisplayName("getByIdPublic() retourne 200 avec le produit")
    void getByIdPublic_shouldReturn200() {
        when(produitService.findById(9L)).thenReturn(produitDto);

        ResponseEntity<ProduitDto> response =
                produitController.getByIdPublic(9L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getNom()).isEqualTo("Le Petit Prince");
    }

    @Test
    @DisplayName("getByCategorie() retourne 200 avec produits filtrés")
    void getByCategorie_shouldReturn200() {
        when(produitService.findByCategorie(1L)).thenReturn(List.of(produitDto));

        ResponseEntity<List<ProduitDto>> response =
                produitController.getByCategorie(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    @DisplayName("search() retourne 200 avec produits trouvés")
    void search_shouldReturn200() {
        when(produitService.search("Prince")).thenReturn(List.of(produitDto));

        ResponseEntity<List<ProduitDto>> response =
                produitController.search("Prince");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    @DisplayName("getEnStock() retourne 200 avec produits en stock")
    void getEnStock_shouldReturn200() {
        when(produitService.findEnStock()).thenReturn(List.of(produitDto));

        ResponseEntity<List<ProduitDto>> response =
                produitController.getEnStock();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("getLowStock() retourne 200 avec produits stock faible")
    void getLowStock_shouldReturn200() {
        when(produitService.findLowStock()).thenReturn(List.of(produitDto));

        ResponseEntity<List<ProduitDto>> response =
                produitController.getLowStock();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("create() sans image retourne 201")
    void create_withoutImage_shouldReturn201() {
        when(produitService.create(any())).thenReturn(produitDto);

        ResponseEntity<ProduitDto> response = produitController.create(
                "Le Petit Prince", "Description", 15.99, 10, 1L, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getNom()).isEqualTo("Le Petit Prince");
    }

    @Test
    @DisplayName("delete() retourne 204 si suppression réussie")
    void delete_shouldReturn204() {
        when(produitService.findById(9L)).thenReturn(produitDto);
        doNothing().when(produitService).delete(9L);

        ResponseEntity<?> response = produitController.delete(9L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(produitService).delete(9L);
    }

    @Test
    @DisplayName("delete() retourne 500 si erreur")
    void delete_shouldReturn500_whenError() {
        when(produitService.findById(9L)).thenThrow(
                new RuntimeException("Erreur"));

        ResponseEntity<?> response = produitController.delete(9L);

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}