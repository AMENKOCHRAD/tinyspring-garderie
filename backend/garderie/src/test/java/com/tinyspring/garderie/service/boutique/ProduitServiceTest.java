package com.tinyspring.garderie.service.boutique;

import com.tinyspring.garderie.dto.boutique.ProduitDto;
import com.tinyspring.garderie.entity.boutique.Categorie;
import com.tinyspring.garderie.entity.boutique.Produit;
import com.tinyspring.garderie.mapper.boutique.ProduitMapper;
import com.tinyspring.garderie.repository.boutique.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests ProduitService")
class ProduitServiceTest {

    @Mock private ProduitRepository produitRepository;
    @Mock private CategorieRepository categorieRepository;
    @Mock private CommandeRepository commandeRepository;
    @Mock private CommandeProduitRepository commandeProduitRepository;
    @Mock private ProduitMapper produitMapper;

    @InjectMocks
    private ProduitServiceImpl produitService;

    private Categorie categorie;
    private Produit produit;
    private ProduitDto produitDto;

    @BeforeEach
    void setUp() {
        categorie = Categorie.builder()
                .id(1L).nom("Livres enfants").build();

        produit = Produit.builder()
                .id(9L)
                .nom("Le Petit Prince")
                .description("Livre classique")
                .prix(15.99)
                .stock(10)
                .seuilAlerte(3)
                .imageUrl("/images/petit-prince.jpg")
                .categorie(categorie)
                .build();

        produitDto = ProduitDto.builder()
                .id(9L)
                .nom("Le Petit Prince")
                .description("Livre classique")
                .prix(15.99)
                .stock(10)
                .seuilAlerte(3)
                .imageUrl("/images/petit-prince.jpg")
                .categorieId(1L)
                .categorieNom("Livres enfants")
                .build();
    }

    // ── findAll ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll() retourne tous les produits")
    void findAll_shouldReturnAllProduits() {
        when(produitRepository.findAll()).thenReturn(List.of(produit));
        when(produitMapper.toDto(produit)).thenReturn(produitDto);

        List<ProduitDto> result = produitService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNom()).isEqualTo("Le Petit Prince");
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById() retourne le produit s'il existe")
    void findById_shouldReturnProduit_whenExists() {
        when(produitRepository.findById(9L)).thenReturn(Optional.of(produit));
        when(produitMapper.toDto(produit)).thenReturn(produitDto);

        ProduitDto result = produitService.findById(9L);

        assertThat(result.getId()).isEqualTo(9L);
        assertThat(result.getPrix()).isEqualTo(15.99);
    }

    @Test
    @DisplayName("findById() lève RuntimeException si produit introuvable")
    void findById_shouldThrow_whenNotFound() {
        when(produitRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> produitService.findById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    // ── findLowStock ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("findLowStock() retourne uniquement les produits sous le seuil")
    void findLowStock_shouldReturnOnlyLowStockProduits() {
        Produit produitOk = Produit.builder()
                .id(2L).nom("Produit OK").stock(10).seuilAlerte(3)
                .categorie(categorie).build();

        Produit produitLow = Produit.builder()
                .id(3L).nom("Produit Low").stock(2).seuilAlerte(3)
                .categorie(categorie).build();

        ProduitDto produitLowDto = ProduitDto.builder()
                .id(3L).nom("Produit Low").stock(2).build();

        when(produitRepository.findAll()).thenReturn(List.of(produitOk, produitLow));
        when(produitMapper.toDto(produitLow)).thenReturn(produitLowDto);

        List<ProduitDto> result = produitService.findLowStock();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNom()).isEqualTo("Produit Low");
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create() sauvegarde et retourne le produit créé")
    void create_shouldSaveAndReturnProduit() {
        when(categorieRepository.findById(1L)).thenReturn(Optional.of(categorie));
        when(produitMapper.toEntity(produitDto)).thenReturn(produit);
        when(produitRepository.save(produit)).thenReturn(produit);
        when(produitMapper.toDto(produit)).thenReturn(produitDto);

        ProduitDto result = produitService.create(produitDto);

        assertThat(result.getNom()).isEqualTo("Le Petit Prince");
        verify(produitRepository).save(produit);
    }

    @Test
    @DisplayName("create() lève RuntimeException si catégorie introuvable")
    void create_shouldThrow_whenCategorieNotFound() {
        when(categorieRepository.findById(99L)).thenReturn(Optional.empty());

        ProduitDto badDto = ProduitDto.builder().categorieId(99L).build();

        assertThatThrownBy(() -> produitService.create(badDto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update() modifie les champs et retourne le produit mis à jour")
    void update_shouldUpdateFields() {
        ProduitDto updateDto = ProduitDto.builder()
                .nom("Nouveau nom").prix(20.0).stock(5)
                .categorieId(1L).build();

        when(produitRepository.findById(9L)).thenReturn(Optional.of(produit));
        when(categorieRepository.findById(1L)).thenReturn(Optional.of(categorie));
        when(produitRepository.save(any())).thenReturn(produit);
        when(produitMapper.toDto(any())).thenReturn(produitDto);

        ProduitDto result = produitService.update(9L, updateDto);

        assertThat(result).isNotNull();
        verify(produitRepository).save(any(Produit.class));
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete() supprime un produit sans commandes liées")
    void delete_shouldDeleteProduit() {
        when(produitRepository.findById(9L)).thenReturn(Optional.of(produit));
        when(commandeRepository.findAll()).thenReturn(List.of());

        produitService.delete(9L);

        verify(produitRepository).delete(produit);
    }

    // ── search ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("search() retourne les produits correspondant au nom")
    void search_shouldReturnMatchingProduits() {
        when(produitRepository.findByNomContainingIgnoreCase("prince"))
                .thenReturn(List.of(produit));
        when(produitMapper.toDto(produit)).thenReturn(produitDto);

        List<ProduitDto> result = produitService.search("prince");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNom()).contains("Prince");
    }
}