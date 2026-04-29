package com.tinyspring.garderie.service.boutique;

import com.tinyspring.garderie.dto.boutique.CategorieDto;
import com.tinyspring.garderie.entity.boutique.Categorie;
import com.tinyspring.garderie.mapper.boutique.CategorieMapper;
import com.tinyspring.garderie.repository.boutique.CategorieRepository;
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
@DisplayName("Tests CategorieService")
class CategorieServiceTest {

    @Mock
    private CategorieRepository categorieRepository;

    @Mock
    private CategorieMapper categorieMapper;

    @InjectMocks
    private CategorieServiceImpl categorieService;

    private Categorie categorie;
    private CategorieDto categorieDto;

    @BeforeEach
    void setUp() {
        categorie = Categorie.builder()
                .id(1L)
                .nom("Livres enfants")
                .description("Livres pour enfants")
                .imageUrl("/images/livres.jpg")
                .build();

        categorieDto = CategorieDto.builder()
                .id(1L)
                .nom("Livres enfants")
                .description("Livres pour enfants")
                .imageUrl("/images/livres.jpg")
                .nombreProduits(0)
                .build();
    }

    // ── findAll ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll() retourne la liste de toutes les catégories")
    void findAll_shouldReturnAllCategories() {
        when(categorieRepository.findAll()).thenReturn(List.of(categorie));
        when(categorieMapper.toDto(categorie)).thenReturn(categorieDto);

        List<CategorieDto> result = categorieService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNom()).isEqualTo("Livres enfants");
        verify(categorieRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("findAll() retourne liste vide si aucune catégorie")
    void findAll_shouldReturnEmptyList() {
        when(categorieRepository.findAll()).thenReturn(List.of());

        List<CategorieDto> result = categorieService.findAll();

        assertThat(result).isEmpty();
        verify(categorieRepository).findAll();
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById() retourne la catégorie si elle existe")
    void findById_shouldReturnCategory_whenExists() {
        when(categorieRepository.findById(1L)).thenReturn(Optional.of(categorie));
        when(categorieMapper.toDto(categorie)).thenReturn(categorieDto);

        CategorieDto result = categorieService.findById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getNom()).isEqualTo("Livres enfants");
    }

    @Test
    @DisplayName("findById() lève RuntimeException si catégorie introuvable")
    void findById_shouldThrow_whenNotFound() {
        when(categorieRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categorieService.findById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create() sauvegarde et retourne la catégorie créée")
    void create_shouldSaveAndReturnCategory() {
        when(categorieRepository.existsByNom("Livres enfants")).thenReturn(false);
        when(categorieMapper.toEntity(categorieDto)).thenReturn(categorie);
        when(categorieRepository.save(categorie)).thenReturn(categorie);
        when(categorieMapper.toDto(categorie)).thenReturn(categorieDto);

        CategorieDto result = categorieService.create(categorieDto);

        assertThat(result.getNom()).isEqualTo("Livres enfants");
        verify(categorieRepository).save(categorie);
    }

    @Test
    @DisplayName("create() lève RuntimeException si nom déjà existant")
    void create_shouldThrow_whenNameAlreadyExists() {
        when(categorieRepository.existsByNom("Livres enfants")).thenReturn(true);

        assertThatThrownBy(() -> categorieService.create(categorieDto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Livres enfants");

        verify(categorieRepository, never()).save(any());
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update() modifie et retourne la catégorie mise à jour")
    void update_shouldUpdateAndReturnCategory() {
        CategorieDto updateDto = CategorieDto.builder()
                .nom("Livres enfants modifié")
                .description("Nouvelle description")
                .imageUrl("/images/new.jpg")
                .build();

        when(categorieRepository.findById(1L)).thenReturn(Optional.of(categorie));
        when(categorieRepository.save(any(Categorie.class))).thenReturn(categorie);
        when(categorieMapper.toDto(any(Categorie.class))).thenReturn(categorieDto);

        CategorieDto result = categorieService.update(1L, updateDto);

        assertThat(result).isNotNull();
        verify(categorieRepository).save(any(Categorie.class));
    }

    @Test
    @DisplayName("update() lève RuntimeException si catégorie introuvable")
    void update_shouldThrow_whenNotFound() {
        when(categorieRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categorieService.update(99L, categorieDto))
                .isInstanceOf(RuntimeException.class);
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete() supprime la catégorie si elle existe")
    void delete_shouldDelete_whenExists() {
        when(categorieRepository.existsById(1L)).thenReturn(true);

        categorieService.delete(1L);

        verify(categorieRepository).deleteById(1L);
    }

    @Test
    @DisplayName("delete() lève RuntimeException si catégorie introuvable")
    void delete_shouldThrow_whenNotFound() {
        when(categorieRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> categorieService.delete(99L))
                .isInstanceOf(RuntimeException.class);

        verify(categorieRepository, never()).deleteById(any());
    }
}