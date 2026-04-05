package com.tinyspring.garderie.repository.boutique;

import com.tinyspring.garderie.entity.boutique.Commande;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommandeRepository extends JpaRepository<Commande, Long> {

    // Toutes les commandes d'un utilisateur
    List<Commande> findByUserId(Long userId);

    // Commandes par statut (admin)
    List<Commande> findByStatut(String statut);

    // Commandes d'un user par statut
    List<Commande> findByUserIdAndStatut(Long userId, String statut);
}
