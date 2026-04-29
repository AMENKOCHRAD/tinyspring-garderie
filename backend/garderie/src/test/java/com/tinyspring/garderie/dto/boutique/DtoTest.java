package com.tinyspring.garderie.dto.boutique;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

@DisplayName("Tests DTOs")
class DtoTest {

    @Test
    @DisplayName("CategorieDto builder et getters")
    void categorieDto_shouldWorkCorrectly() {
        CategorieDto dto = CategorieDto.builder()
                .id(1L).nom("Livres").description("Desc")
                .imageUrl("/img.jpg").nombreProduits(5).build();

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getNom()).isEqualTo("Livres");
        assertThat(dto.getDescription()).isEqualTo("Desc");
        assertThat(dto.getImageUrl()).isEqualTo("/img.jpg");
        assertThat(dto.getNombreProduits()).isEqualTo(5);
    }

    @Test
    @DisplayName("ProduitDto builder et getters")
    void produitDto_shouldWorkCorrectly() {
        ProduitDto dto = ProduitDto.builder()
                .id(1L).nom("Produit").description("Desc")
                .prix(10.0).stock(5).seuilAlerte(3)
                .imageUrl("/img.jpg").categorieId(1L)
                .categorieNom("Cat").build();

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getNom()).isEqualTo("Produit");
        assertThat(dto.getPrix()).isEqualTo(10.0);
        assertThat(dto.getStock()).isEqualTo(5);
        assertThat(dto.getCategorieId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("CommandeDto builder et getters")
    void commandeDto_shouldWorkCorrectly() {
        CommandeItemDto item = CommandeItemDto.builder()
                .produitId(1L).produitNom("Produit")
                .quantite(2).prixUnitaire(10.0).sousTotal(20.0).build();

        CommandeDto dto = CommandeDto.builder()
                .id(1L).statut("PENDING")
                .paymentStatus("PENDING")
                .montantTotal(20.0)
                .userId(1L).userNom("User")
                .userEmail("user@test.com")
                .items(List.of(item)).build();

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getStatut()).isEqualTo("PENDING");
        assertThat(dto.getItems()).hasSize(1);
        assertThat(dto.getItems().get(0).getSousTotal()).isEqualTo(20.0);
    }

    @Test
    @DisplayName("CommandeRequest builder et getters")
    void commandeRequest_shouldWorkCorrectly() {
        CommandeItemRequest itemReq = new CommandeItemRequest(1L, 3);
        CommandeRequest req = CommandeRequest.builder()
                .userId(1L).adresseLivraison("123 rue Test")
                .items(List.of(itemReq)).build();

        assertThat(req.getUserId()).isEqualTo(1L);
        assertThat(req.getAdresseLivraison()).isEqualTo("123 rue Test");
        assertThat(req.getItems()).hasSize(1);
        assertThat(req.getItems().get(0).getProduitId()).isEqualTo(1L);
        assertThat(req.getItems().get(0).getQuantite()).isEqualTo(3);
    }

    @Test
    @DisplayName("DashboardStatsDto builder et getters")
    void dashboardStatsDto_shouldWorkCorrectly() {
        DashboardRecentCommandeDto recent = DashboardRecentCommandeDto.builder()
                .id(1L).statut("CONFIRMEE").montantTotal(99.0)
                .userNom("Test").userEmail("t@t.com")
                .dateCommande(LocalDateTime.now()).build();

        DashboardTopProduitDto top = DashboardTopProduitDto.builder()
                .id(1L).nom("Produit").imageUrl("/img.jpg")
                .totalCommandes(10L).build();

        DashboardMonthlySalesDto monthly = DashboardMonthlySalesDto.builder()
                .mois("2026-01").montant(500.0).build();

        DashboardStatsDto stats = DashboardStatsDto.builder()
                .totalProduits(10L).produitsEnStock(8L)
                .produitsRupture(2L).produitsStockFaible(1L)
                .totalCategories(5L).totalCommandes(20L)
                .commandesEnAttente(2L).commandesConfirmees(10L)
                .commandesExpediees(5L).commandesLivrees(3L)
                .commandesAnnulees(0L).montantTotalVentes(1000.0)
                .dernieresCommandes(List.of(recent))
                .topProduits(List.of(top))
                .ventesParMois(List.of(monthly)).build();

        assertThat(stats.getTotalProduits()).isEqualTo(10L);
        assertThat(stats.getMontantTotalVentes()).isEqualTo(1000.0);
        assertThat(stats.getDernieresCommandes()).hasSize(1);
        assertThat(stats.getTopProduits()).hasSize(1);
        assertThat(stats.getVentesParMois()).hasSize(1);
    }

    @Test
    @DisplayName("UserCategorieScoreAdminDto builder et getters")
    void userCategorieScoreAdminDto_shouldWorkCorrectly() {
        UserCategorieScoreAdminDto dto = UserCategorieScoreAdminDto.builder()
                .id(1L).userId(1L).userNom("Test")
                .userEmail("t@t.com").categorieId(1L)
                .categorieNom("Cat").score(50.0)
                .nbInteractions(5)
                .derniereInteraction(LocalDateTime.now()).build();

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getScore()).isEqualTo(50.0);
        assertThat(dto.getNbInteractions()).isEqualTo(5);
    }

    @Test
    @DisplayName("InteractionRequestDto setters et getters")
    void interactionRequestDto_shouldWorkCorrectly() {
        InteractionRequestDto dto = new InteractionRequestDto();
        dto.setProduitId(1L);
        dto.setTypeInteraction("VUE_3S");

        assertThat(dto.getProduitId()).isEqualTo(1L);
        assertThat(dto.getTypeInteraction()).isEqualTo("VUE_3S");
    }
}