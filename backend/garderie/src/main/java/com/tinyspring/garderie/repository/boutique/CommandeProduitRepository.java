package com.tinyspring.garderie.repository.boutique;

import com.tinyspring.garderie.entity.boutique.CommandeProduit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommandeProduitRepository extends JpaRepository<CommandeProduit, Long> {

    List<CommandeProduit> findByCommandeId(Long commandeId);
}
