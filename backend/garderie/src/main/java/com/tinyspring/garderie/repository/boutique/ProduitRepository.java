package com.tinyspring.garderie.repository.boutique;

import com.tinyspring.garderie.entity.boutique.Produit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProduitRepository extends JpaRepository<Produit, Long> {

    // Tous les produits d'une catégorie
    List<Produit> findByCategorieId(Long categorieId);

    // Recherche par nom (insensible à la casse)
    List<Produit> findByNomContainingIgnoreCase(String nom);

    // Produits encore en stock
    List<Produit> findByStockGreaterThan(int stock);

    boolean existsByNomAndCategorieId(String nom, Long categorieId);
}
