package com.tinyspring.garderie.repository;

import com.tinyspring.garderie.entity.Traitement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TraitementRepository extends JpaRepository<Traitement, Long> {

    // Lister tous les traitements d'une condition sanitaire
    List<Traitement> findByConditionSanitaireId(Long conditionId);

    // Lister tous les traitements d'un enfant via les conditions sanitaires
    List<Traitement> findByConditionSanitaire_EnfantId(Long enfantId);
}