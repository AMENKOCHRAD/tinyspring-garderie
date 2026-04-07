package com.tinyspring.garderie.repository.boutique;

import com.tinyspring.garderie.entity.boutique.Produit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProduitRepository extends JpaRepository<Produit, Long> {

    List<Produit> findByCategorieId(Long categorieId);

    List<Produit> findByNomContainingIgnoreCase(String nom);

    List<Produit> findByStockGreaterThan(int stock);

    boolean existsByNomAndCategorieId(String nom, Long categorieId);

    // Vérifie si le produit est lié à au moins une commande
    @Query("SELECT COUNT(p) > 0 FROM Produit p JOIN p.commandes c WHERE p.id = :produitId")
    boolean existsInCommandes(@Param("produitId") Long produitId);
}
