package com.tinyspring.garderie.repository.boutique;

import com.tinyspring.garderie.entity.boutique.CommandeProduit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CommandeProduitRepository extends JpaRepository<CommandeProduit, Long> {

    List<CommandeProduit> findByCommandeId(Long commandeId);
    @Query("SELECT COUNT(cp) FROM CommandeProduit cp " +
            "JOIN cp.commande c " +
            "WHERE cp.produit.id = :produitId " +
            "AND c.dateCommande >= :depuis " +
            "AND c.statut IN ('CONFIRMEE','EXPEDIEE','LIVREE')")
    int countVentesDepuis(@Param("produitId") Long produitId,
                          @Param("depuis") LocalDateTime depuis);
}
