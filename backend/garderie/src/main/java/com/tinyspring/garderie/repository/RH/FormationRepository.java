package com.tinyspring.garderie.repository.RH;

import com.tinyspring.garderie.entity.RH.Formation;
import com.tinyspring.garderie.entity.RH.enums.StatutFormation;
import com.tinyspring.garderie.entity.RH.enums.TypeFormation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FormationRepository extends JpaRepository<Formation, Long> {

    List<Formation> findByStatut(StatutFormation statut);

    List<Formation> findByType(TypeFormation type);

    List<Formation> findByObligatoire(Boolean obligatoire);

    List<Formation> findByStatutOrderByDateFormationAsc(StatutFormation statut);

    // ✅ dateDebut → dateFormation
    @Query("SELECT f FROM Formation f WHERE f.dateFormation BETWEEN :today AND :inSevenDays AND f.statut = 'OUVERTE'")
    List<Formation> findFormationsBientotDebutees(LocalDate today, LocalDate inSevenDays);

    // Formations sous-inscrites
    @Query("SELECT f FROM Formation f WHERE f.statut = 'OUVERTE' AND f.placesMax IS NOT NULL AND SIZE(f.inscriptions) < (f.placesMax * 0.5)")
    List<Formation> findFormationsSousInscrites();

    long countByStatut(StatutFormation statut);

    List<Formation> findByTypeAndStatut(TypeFormation type, StatutFormation statut);
}