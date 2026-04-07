package com.tinyspring.garderie.repository;

import com.tinyspring.garderie.entity.StatutTraitement;
import com.tinyspring.garderie.entity.Traitement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TraitementRepository extends JpaRepository<Traitement, Long> {

    List<Traitement> findByConditionSanitaireId(Long conditionId);

    List<Traitement> findByConditionSanitaireEnfantId(Long enfantId);

    List<Traitement> findByStatut(StatutTraitement statut);
}