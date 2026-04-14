package com.tinyspring.garderie.repository.boutique;

import com.tinyspring.garderie.entity.boutique.Commande;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommandeRepository extends JpaRepository<Commande, Long> {

    List<Commande> findByUserId(Long userId);

    List<Commande> findByStatut(String statut);

    List<Commande> findByUserIdAndStatut(Long userId, String statut);

    // ✅ Nécessaire pour le webhook Stripe
    Optional<Commande> findByStripeSessionId(String stripeSessionId);

    long countByStatut(String statut);

    @Query("SELECT COALESCE(SUM(c.montantTotal), 0) FROM Commande c WHERE c.statut <> 'ANNULEE'")
    Double sumMontantTotalCommandesValides();

    List<Commande> findAllByOrderByDateCommandeDesc(Pageable pageable);

    @Query("""
        SELECT FUNCTION('DATE_FORMAT', c.dateCommande, '%Y-%m'),
               COALESCE(SUM(c.montantTotal), 0)
        FROM Commande c
        WHERE c.statut <> 'ANNULEE'
        GROUP BY FUNCTION('DATE_FORMAT', c.dateCommande, '%Y-%m')
        ORDER BY FUNCTION('DATE_FORMAT', c.dateCommande, '%Y-%m')
    """)
    List<Object[]> sumMontantTotalGroupByMonth();
}
