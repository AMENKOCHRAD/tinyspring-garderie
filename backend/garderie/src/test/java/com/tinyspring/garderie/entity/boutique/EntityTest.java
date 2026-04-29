package com.tinyspring.garderie.entity.boutique;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

@DisplayName("Tests Entities boutique")
class EntityTest {

    @Test
    @DisplayName("Categorie builder et getters")
    void categorie_shouldWorkCorrectly() {
        Categorie categorie = Categorie.builder()
                .id(1L).nom("Livres").description("Desc")
                .imageUrl("/img.jpg").build();

        assertThat(categorie.getId()).isEqualTo(1L);
        assertThat(categorie.getNom()).isEqualTo("Livres");
        categorie.setNom("Modifié");
        assertThat(categorie.getNom()).isEqualTo("Modifié");
    }

    @Test
    @DisplayName("Produit builder et getters")
    void produit_shouldWorkCorrectly() {
        Categorie cat = Categorie.builder().id(1L).nom("Cat").build();
        Produit produit = Produit.builder()
                .id(1L).nom("Produit").prix(10.0)
                .stock(5).seuilAlerte(3)
                .imageUrl("/img.jpg").categorie(cat).build();

        assertThat(produit.getId()).isEqualTo(1L);
        assertThat(produit.getPrix()).isEqualTo(10.0);
        assertThat(produit.getCategorie().getNom()).isEqualTo("Cat");
        produit.setStock(10);
        assertThat(produit.getStock()).isEqualTo(10);
    }

    @Test
    @DisplayName("CommandeProduit getSousTotal calcule correctement")
    void commandeProduit_getSousTotal() {
        CommandeProduit cp = CommandeProduit.builder()
                .id(1L).quantite(3).prixUnitaire(10.0).build();

        assertThat(cp.getSousTotal()).isEqualTo(30.0);
    }
}