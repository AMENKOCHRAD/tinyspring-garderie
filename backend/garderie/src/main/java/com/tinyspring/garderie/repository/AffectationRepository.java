package com.tinyspring.garderie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tinyspring.garderie.entity.Affectation;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface AffectationRepository extends JpaRepository<Affectation, Long> {
    int countByGroupeIdAndStatut(Long groupeId, String statut);

    @Query("SELECT COUNT(a) FROM Affectation a JOIN a.groupe g WHERE g.classe.id = :classeId AND a.statut = 'ACTIF'")
    int countActiveEnfantsByClasseId(@Param("classeId") Long classeId);

    @Query("SELECT a FROM Affectation a JOIN a.groupe g WHERE g.classe.id = :classeId AND a.statut = 'ACTIF'")
    List<Affectation> findActiveByClasseId(@Param("classeId") Long classeId);

    @Query("SELECT a FROM Affectation a WHERE a.enfantId = :enfantId AND a.statut = 'ACTIF'")
    List<Affectation> findActiveByEnfantId(@Param("enfantId") Long enfantId);
}
