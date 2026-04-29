package com.tinyspring.garderie.service.boutique;

import com.tinyspring.garderie.dto.boutique.InteractionRequestDto;
import com.tinyspring.garderie.dto.boutique.ProduitDto;
import com.tinyspring.garderie.dto.boutique.UserCategorieScoreAdminDto;
import com.tinyspring.garderie.entity.boutique.*;
import com.tinyspring.garderie.entity.User;
import com.tinyspring.garderie.mapper.boutique.AffiniteMapper;
import com.tinyspring.garderie.repository.UserRepository;
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
@DisplayName("Tests AffiniteService")
class AffiniteServiceTest {

    @Mock private UserCategorieScoreRepository scoreRepo;
    @Mock private UserInteractionRepository interactionRepo;
    @Mock private ProduitRepository produitRepo;
    @Mock private UserRepository userRepo;
    @Mock private AffiniteMapper affiniteMapper;

    @InjectMocks
    private AffiniteServiceImpl affiniteService;

    private User user;
    private Categorie categorie;
    private Produit produit;
    private UserCategorieScore score;

    @BeforeEach
    void setUp() {
        user = new User("Parent Test", "parent@test.com", "password123", true, null);

        categorie = Categorie.builder()
                .id(1L).nom("Livres enfants").build();

        produit = Produit.builder()
                .id(9L).nom("Le Petit Prince")
                .stock(10).categorie(categorie).build();

        score = UserCategorieScore.builder()
                .id(1L).user(user).categorie(categorie)
                .score(50.0).nbInteractions(5).build();
    }

    // ── enregistrerInteraction ────────────────────────────────────────────────

    @Test
    @DisplayName("enregistrerInteraction() ignore les types inconnus (points = 0)")
    void enregistrerInteraction_shouldIgnore_whenTypeInconnu() {
        InteractionRequestDto dto = new InteractionRequestDto();
        dto.setProduitId(9L);
        dto.setTypeInteraction("TYPE_INCONNU");

        affiniteService.enregistrerInteraction(1L, dto);

        verify(produitRepo, never()).findById(any());
        verify(interactionRepo, never()).save(any());
    }

    @Test
    @DisplayName("enregistrerInteraction() lève RuntimeException si produit introuvable")
    void enregistrerInteraction_shouldThrow_whenProduitNotFound() {
        InteractionRequestDto dto = new InteractionRequestDto();
        dto.setProduitId(99L);
        dto.setTypeInteraction("VUE_3S");

        when(produitRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> affiniteService.enregistrerInteraction(1L, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Produit introuvable");
    }

    @Test
    @DisplayName("enregistrerInteraction() lève RuntimeException si user introuvable")
    void enregistrerInteraction_shouldThrow_whenUserNotFound() {
        InteractionRequestDto dto = new InteractionRequestDto();
        dto.setProduitId(9L);
        dto.setTypeInteraction("VUE_3S");

        when(produitRepo.findById(9L)).thenReturn(Optional.of(produit));
        when(userRepo.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> affiniteService.enregistrerInteraction(1L, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User introuvable");
    }

    @Test
    @DisplayName("enregistrerInteraction() crée un nouveau score si inexistant")
    void enregistrerInteraction_shouldCreateNewScore_whenNotExists() {
        InteractionRequestDto dto = new InteractionRequestDto();
        dto.setProduitId(9L);
        dto.setTypeInteraction("COMMANDE");

        when(produitRepo.findById(9L)).thenReturn(Optional.of(produit));
        when(userRepo.findById(1L)).thenReturn(Optional.of(user));
        when(scoreRepo.findByUserIdAndCategorieId(1L, 1L))
                .thenReturn(Optional.empty());
        when(interactionRepo.save(any())).thenReturn(null);
        when(scoreRepo.save(any())).thenReturn(score);

        affiniteService.enregistrerInteraction(1L, dto);

        verify(scoreRepo).save(any(UserCategorieScore.class));
        verify(interactionRepo).save(any(UserInteraction.class));
    }

    @Test
    @DisplayName("enregistrerInteraction() met à jour le score existant")
    void enregistrerInteraction_shouldUpdateScore_whenExists() {
        InteractionRequestDto dto = new InteractionRequestDto();
        dto.setProduitId(9L);
        dto.setTypeInteraction("AJOUT_PANIER");

        when(produitRepo.findById(9L)).thenReturn(Optional.of(produit));
        when(userRepo.findById(1L)).thenReturn(Optional.of(user));
        when(scoreRepo.findByUserIdAndCategorieId(1L, 1L))
                .thenReturn(Optional.of(score));
        when(interactionRepo.save(any())).thenReturn(null);
        when(scoreRepo.save(any())).thenReturn(score);

        affiniteService.enregistrerInteraction(1L, dto);

        verify(scoreRepo).save(score);
        assertThat(score.getNbInteractions()).isEqualTo(6);
    }

    // ── getProduitRecommandes ─────────────────────────────────────────────────

    @Test
    @DisplayName("getProduitRecommandes() retourne tous les produits si nouvel utilisateur")
    void getProduitRecommandes_shouldReturnAll_whenNewUser() {
        ProduitDto produitDto = ProduitDto.builder()
                .id(9L).nom("Le Petit Prince").build();

        when(scoreRepo.findByUserIdOrderByScoreDesc(1L)).thenReturn(List.of());
        when(produitRepo.findByStockGreaterThan(0)).thenReturn(List.of(produit));
        when(affiniteMapper.toProduitDto(produit)).thenReturn(produitDto);

        List<ProduitDto> result = affiniteService.getProduitRecommandes(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNom()).isEqualTo("Le Petit Prince");
    }

    @Test
    @DisplayName("getProduitRecommandes() trie par score si utilisateur connu")
    void getProduitRecommandes_shouldSortByScore_whenUserHasScores() {
        ProduitDto produitDto = ProduitDto.builder()
                .id(9L).nom("Le Petit Prince").build();

        when(scoreRepo.findByUserIdOrderByScoreDesc(1L)).thenReturn(List.of(score));
        when(produitRepo.findByStockGreaterThan(0)).thenReturn(List.of(produit));
        when(affiniteMapper.toProduitDto(produit)).thenReturn(produitDto);

        List<ProduitDto> result = affiniteService.getProduitRecommandes(1L);

        assertThat(result).hasSize(1);
    }

    // ── getScoresUser ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getScoresUser() retourne les scores triés par score desc")
    void getScoresUser_shouldReturnSortedScores() {
        when(scoreRepo.findByUserIdOrderByScoreDesc(1L)).thenReturn(List.of(score));

        List<UserCategorieScore> result = affiniteService.getScoresUser(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getScore()).isEqualTo(50.0);
    }

    // ── getAllScoresAdmin ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllScoresAdmin() retourne tous les scores triés")
    void getAllScoresAdmin_shouldReturnAllScoresSorted() {
        UserCategorieScoreAdminDto adminDto = UserCategorieScoreAdminDto.builder()
                .id(1L).score(50.0).build();

        when(scoreRepo.findAll()).thenReturn(List.of(score));
        when(affiniteMapper.toAdminDto(score)).thenReturn(adminDto);

        List<UserCategorieScoreAdminDto> result = affiniteService.getAllScoresAdmin();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getScore()).isEqualTo(50.0);
    }

    // ── getScoresByUserAdmin ──────────────────────────────────────────────────

    @Test
    @DisplayName("getScoresByUserAdmin() retourne les scores d'un user spécifique")
    void getScoresByUserAdmin_shouldReturnUserScores() {
        UserCategorieScoreAdminDto adminDto = UserCategorieScoreAdminDto.builder()
                .id(1L).userId(1L).score(50.0).build();

        when(scoreRepo.findByUserIdOrderByScoreDesc(1L)).thenReturn(List.of(score));
        when(affiniteMapper.toAdminDto(score)).thenReturn(adminDto);

        List<UserCategorieScoreAdminDto> result =
                affiniteService.getScoresByUserAdmin(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(1L);
    }
}