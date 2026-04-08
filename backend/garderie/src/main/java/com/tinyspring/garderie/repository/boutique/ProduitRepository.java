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

    long countByStockGreaterThan(int stock);

    long countByStockEquals(int stock);

    @Query("SELECT COUNT(p) FROM Produit p WHERE p.stock <= p.seuilAlerte")
    long countLowStockProduits();

    @Query("""
    SELECT p.id, p.nom, p.imageUrl, COUNT(c)
    FROM Produit p
    JOIN p.commandes c
    GROUP BY p.id, p.nom, p.imageUrl
    ORDER BY COUNT(c) DESC
""")
    List<Object[]> findTopProduitsByCommandes(org.springframework.data.domain.Pageable pageable);
}
