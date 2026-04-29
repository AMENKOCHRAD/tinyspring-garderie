package com.tinyspring.garderie.repository;

import com.tinyspring.garderie.entity.TraitementValidationEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TraitementValidationEventRepository extends JpaRepository<TraitementValidationEvent, Long> {
    List<TraitementValidationEvent> findTop100ByTraitementIdOrderByCreeLeDesc(Long traitementId);

    @Query("""
            select e from TraitementValidationEvent e
            join fetch e.traitement t
            join fetch t.conditionSanitaire cs
            join fetch cs.enfant enf
            join fetch enf.parent p
            order by e.creeLe desc
            """)
    List<TraitementValidationEvent> findLatestWithDetails(Pageable pageable);
}
