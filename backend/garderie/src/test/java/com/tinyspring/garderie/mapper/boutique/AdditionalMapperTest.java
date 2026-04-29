package com.tinyspring.garderie.mapper.boutique;

import com.tinyspring.garderie.dto.boutique.CommandeDto;
import com.tinyspring.garderie.dto.boutique.CommandeItemDto;
import com.tinyspring.garderie.dto.boutique.DashboardRecentCommandeDto;
import com.tinyspring.garderie.dto.boutique.ProduitDto;
import com.tinyspring.garderie.dto.boutique.UserCategorieScoreAdminDto;
import com.tinyspring.garderie.entity.User;
import com.tinyspring.garderie.entity.boutique.Categorie;
import com.tinyspring.garderie.entity.boutique.Commande;
import com.tinyspring.garderie.entity.boutique.CommandeProduit;
import com.tinyspring.garderie.entity.boutique.Produit;
import com.tinyspring.garderie.entity.boutique.UserCategorieScore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests mappers boutique additionnels")
class AdditionalMapperTest {

    private final CommandeItemMapper commandeItemMapper =
            Mappers.getMapper(CommandeItemMapper.class);
    private final CommandeMapper commandeMapper =
            Mappers.getMapper(CommandeMapper.class);
    private final DashboardMapper dashboardMapper =
            Mappers.getMapper(DashboardMapper.class);
    private final AffiniteMapper affiniteMapper =
            Mappers.getMapper(AffiniteMapper.class);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(
                commandeMapper,
                "commandeItemMapper",
                commandeItemMapper
        );
    }

    @Test
    @DisplayName("CommandeItemMapper.toDto() mappe un item complet")
    void commandeItemMapper_shouldMapFullItem() {
        Produit produit = Produit.builder()
                .id(5L)
                .nom("Puzzle bois")
                .imageUrl("/images/boutique/puzzle.png")
                .build();
        CommandeProduit item = CommandeProduit.builder()
                .produit(produit)
                .quantite(3)
                .prixUnitaire(12.50)
                .build();

        CommandeItemDto result = commandeItemMapper.toDto(item);

        assertThat(result.getProduitId()).isEqualTo(5L);
        assertThat(result.getProduitNom()).isEqualTo("Puzzle bois");
        assertThat(result.getProduitImageUrl()).isEqualTo("/images/boutique/puzzle.png");
        assertThat(result.getQuantite()).isEqualTo(3);
        assertThat(result.getPrixUnitaire()).isEqualTo(12.50);
        assertThat(result.getSousTotal()).isEqualTo(37.50);
    }

    @Test
    @DisplayName("CommandeItemMapper.toDto() retourne null pour une entree null")
    void commandeItemMapper_shouldReturnNullForNullInput() {
        assertThat(commandeItemMapper.toDto(null)).isNull();
    }

    @Test
    @DisplayName("CommandeItemMapper.toDto() accepte un produit absent")
    void commandeItemMapper_shouldMapItemWithoutProduct() {
        CommandeProduit item = CommandeProduit.builder()
                .quantite(2)
                .prixUnitaire(8.0)
                .build();

        CommandeItemDto result = commandeItemMapper.toDto(item);

        assertThat(result.getProduitId()).isNull();
        assertThat(result.getProduitNom()).isNull();
        assertThat(result.getProduitImageUrl()).isNull();
        assertThat(result.getSousTotal()).isEqualTo(16.0);
    }

    @Test
    @DisplayName("CommandeMapper.toDto() mappe commande, utilisateur et items")
    void commandeMapper_shouldMapCommandeWithUserAndItems() {
        User user = user(7L, "Parent Test", "parent@example.com");
        Produit produit = Produit.builder()
                .id(5L)
                .nom("Puzzle bois")
                .imageUrl("/images/boutique/puzzle.png")
                .build();
        CommandeProduit item = CommandeProduit.builder()
                .produit(produit)
                .quantite(2)
                .prixUnitaire(11.0)
                .build();
        LocalDateTime dateCommande = LocalDateTime.of(2026, 4, 29, 9, 0);
        Commande commande = Commande.builder()
                .id(42L)
                .dateCommande(dateCommande)
                .statut("CONFIRMEE")
                .paymentStatus("PAID")
                .montantTotal(22.0)
                .adresseLivraison("12 rue des Fleurs")
                .stripeSessionId("sess_123")
                .stripePaymentIntentId("pi_123")
                .user(user)
                .items(new ArrayList<>(List.of(item)))
                .build();

        CommandeDto result = commandeMapper.toDto(commande);

        assertThat(result.getId()).isEqualTo(42L);
        assertThat(result.getDateCommande()).isEqualTo(dateCommande);
        assertThat(result.getStatut()).isEqualTo("CONFIRMEE");
        assertThat(result.getPaymentStatus()).isEqualTo("PAID");
        assertThat(result.getMontantTotal()).isEqualTo(22.0);
        assertThat(result.getAdresseLivraison()).isEqualTo("12 rue des Fleurs");
        assertThat(result.getStripeSessionId()).isEqualTo("sess_123");
        assertThat(result.getStripePaymentIntentId()).isEqualTo("pi_123");
        assertThat(result.getUserId()).isEqualTo(7L);
        assertThat(result.getUserNom()).isEqualTo("Parent Test");
        assertThat(result.getUserEmail()).isEqualTo("parent@example.com");
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getSousTotal()).isEqualTo(22.0);
    }

    @Test
    @DisplayName("CommandeMapper.toDto() retourne null pour une entree null")
    void commandeMapper_shouldReturnNullForNullInput() {
        assertThat(commandeMapper.toDto(null)).isNull();
    }

    @Test
    @DisplayName("CommandeMapper.toDto() accepte utilisateur et items absents")
    void commandeMapper_shouldMapCommandeWithoutUserAndItems() {
        Commande commande = Commande.builder()
                .id(42L)
                .items(null)
                .build();

        CommandeDto result = commandeMapper.toDto(commande);

        assertThat(result.getId()).isEqualTo(42L);
        assertThat(result.getUserId()).isNull();
        assertThat(result.getUserNom()).isNull();
        assertThat(result.getUserEmail()).isNull();
        assertThat(result.getItems()).isNull();
    }

    @Test
    @DisplayName("DashboardMapper.toRecentCommandeDto() mappe une commande recente")
    void dashboardMapper_shouldMapRecentCommande() {
        User user = user(8L, "Admin Parent", "admin-parent@example.com");
        LocalDateTime dateCommande = LocalDateTime.of(2026, 4, 29, 11, 15);
        Commande commande = Commande.builder()
                .id(99L)
                .dateCommande(dateCommande)
                .statut("LIVREE")
                .montantTotal(75.0)
                .user(user)
                .items(new ArrayList<>())
                .build();

        DashboardRecentCommandeDto result =
                dashboardMapper.toRecentCommandeDto(commande);

        assertThat(result.getId()).isEqualTo(99L);
        assertThat(result.getDateCommande()).isEqualTo(dateCommande);
        assertThat(result.getStatut()).isEqualTo("LIVREE");
        assertThat(result.getMontantTotal()).isEqualTo(75.0);
        assertThat(result.getUserNom()).isEqualTo("Admin Parent");
        assertThat(result.getUserEmail()).isEqualTo("admin-parent@example.com");
    }

    @Test
    @DisplayName("DashboardMapper.toRecentCommandeDto() retourne null pour null")
    void dashboardMapper_shouldReturnNullForNullInput() {
        assertThat(dashboardMapper.toRecentCommandeDto(null)).isNull();
    }

    @Test
    @DisplayName("DashboardMapper.toRecentCommandeDto() accepte un utilisateur absent")
    void dashboardMapper_shouldMapCommandeWithoutUser() {
        Commande commande = Commande.builder()
                .id(99L)
                .items(new ArrayList<>())
                .build();

        DashboardRecentCommandeDto result =
                dashboardMapper.toRecentCommandeDto(commande);

        assertThat(result.getId()).isEqualTo(99L);
        assertThat(result.getUserNom()).isNull();
        assertThat(result.getUserEmail()).isNull();
    }

    @Test
    @DisplayName("AffiniteMapper.toProduitDto() mappe le produit recommande")
    void affiniteMapper_shouldMapProduitDto() {
        Categorie categorie = Categorie.builder()
                .id(4L)
                .nom("Jeux educatifs")
                .build();
        Produit produit = Produit.builder()
                .id(5L)
                .nom("Puzzle bois")
                .description("Puzzle 24 pieces")
                .prix(12.50)
                .stock(8)
                .imageUrl("/images/boutique/puzzle.png")
                .seuilAlerte(2)
                .categorie(categorie)
                .build();

        ProduitDto result = affiniteMapper.toProduitDto(produit);

        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getNom()).isEqualTo("Puzzle bois");
        assertThat(result.getDescription()).isEqualTo("Puzzle 24 pieces");
        assertThat(result.getPrix()).isEqualTo(12.50);
        assertThat(result.getStock()).isEqualTo(8);
        assertThat(result.getImageUrl()).isEqualTo("/images/boutique/puzzle.png");
        assertThat(result.getSeuilAlerte()).isEqualTo(2);
        assertThat(result.getCategorieId()).isEqualTo(4L);
        assertThat(result.getCategorieNom()).isEqualTo("Jeux educatifs");
    }

    @Test
    @DisplayName("AffiniteMapper.toProduitDto() retourne null pour null")
    void affiniteMapper_shouldReturnNullForNullProduit() {
        assertThat(affiniteMapper.toProduitDto(null)).isNull();
    }

    @Test
    @DisplayName("AffiniteMapper.toProduitDto() accepte une categorie absente")
    void affiniteMapper_shouldMapProduitWithoutCategorie() {
        Produit produit = Produit.builder()
                .id(5L)
                .nom("Puzzle bois")
                .build();

        ProduitDto result = affiniteMapper.toProduitDto(produit);

        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getCategorieId()).isNull();
        assertThat(result.getCategorieNom()).isNull();
    }

    @Test
    @DisplayName("AffiniteMapper.toAdminDto() mappe un score utilisateur categorie")
    void affiniteMapper_shouldMapAdminScore() {
        User user = user(7L, "Parent Test", "parent@example.com");
        Categorie categorie = Categorie.builder()
                .id(4L)
                .nom("Jeux educatifs")
                .build();
        LocalDateTime derniereInteraction =
                LocalDateTime.of(2026, 4, 29, 12, 0);
        UserCategorieScore score = UserCategorieScore.builder()
                .id(15L)
                .user(user)
                .categorie(categorie)
                .score(8.5)
                .nbInteractions(6)
                .derniereInteraction(derniereInteraction)
                .build();

        UserCategorieScoreAdminDto result = affiniteMapper.toAdminDto(score);

        assertThat(result.getId()).isEqualTo(15L);
        assertThat(result.getUserId()).isEqualTo(7L);
        assertThat(result.getUserNom()).isEqualTo("Parent Test");
        assertThat(result.getUserEmail()).isEqualTo("parent@example.com");
        assertThat(result.getCategorieId()).isEqualTo(4L);
        assertThat(result.getCategorieNom()).isEqualTo("Jeux educatifs");
        assertThat(result.getScore()).isEqualTo(8.5);
        assertThat(result.getNbInteractions()).isEqualTo(6);
        assertThat(result.getDerniereInteraction()).isEqualTo(derniereInteraction);
    }

    @Test
    @DisplayName("AffiniteMapper.toAdminDto() retourne null pour null")
    void affiniteMapper_shouldReturnNullForNullAdminScore() {
        assertThat(affiniteMapper.toAdminDto(null)).isNull();
    }

    @Test
    @DisplayName("AffiniteMapper.toAdminDto() accepte utilisateur et categorie absents")
    void affiniteMapper_shouldMapScoreWithoutRelations() {
        UserCategorieScore score = UserCategorieScore.builder()
                .id(15L)
                .score(2.0)
                .nbInteractions(1)
                .build();

        UserCategorieScoreAdminDto result = affiniteMapper.toAdminDto(score);

        assertThat(result.getId()).isEqualTo(15L);
        assertThat(result.getUserId()).isNull();
        assertThat(result.getUserNom()).isNull();
        assertThat(result.getUserEmail()).isNull();
        assertThat(result.getCategorieId()).isNull();
        assertThat(result.getCategorieNom()).isNull();
    }

    private User user(Long id, String nom, String email) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", id);
        user.setNom(nom);
        user.setEmail(email);
        user.setPassword("secret");
        user.setEnabled(true);
        return user;
    }
}
