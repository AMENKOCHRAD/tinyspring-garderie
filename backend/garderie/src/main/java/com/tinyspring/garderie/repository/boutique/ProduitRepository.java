package com.tinyspring.garderie.repository.boutique;

import com.tinyspring.garderie.entity.boutique.Produit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    long countByStockGreaterThan(int stock);

    long countByStockEquals(int stock);

    @Query("SELECT COUNT(p) FROM Produit p WHERE p.stock <= p.seuilAlerte")
    long countLowStockProduits();

    // ✅ Vérifie si le produit est lié à une commande via CommandeProduit
    @Query("SELECT COUNT(cp) > 0 FROM CommandeProduit cp WHERE cp.produit.id = :produitId")
    boolean existsInCommandes(@Param("produitId") Long produitId);



    // ✅ Top produits via CommandeProduit (plus de p.commandes)
    @Query("""
        SELECT cp.produit.id, cp.produit.nom, cp.produit.imageUrl, SUM(cp.quantite)
        FROM CommandeProduit cp
        GROUP BY cp.produit.id, cp.produit.nom, cp.produit.imageUrl
        ORDER BY SUM(cp.quantite) DESC
    """)
    List<Object[]> findTopProduitsByCommandes(Pageable pageable);

    Page<Produit> findByNomContainingIgnoreCase(String nom, Pageable pageable);
    Page<Produit> findByCategorieId(Long categorieId, Pageable pageable);



}