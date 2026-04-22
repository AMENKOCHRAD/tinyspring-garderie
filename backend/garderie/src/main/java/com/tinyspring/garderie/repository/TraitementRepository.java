package com.tinyspring.garderie.repository;

import com.tinyspring.garderie.entity.StatutTraitement;
import com.tinyspring.garderie.entity.Traitement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TraitementRepository extends JpaRepository<Traitement, Long> {

    List<Traitement> findByConditionSanitaireId(Long conditionId);

    List<Traitement> findByConditionSanitaireEnfantId(Long enfantId);

    List<Traitement> findByStatut(StatutTraitement statut);

    @Query("""
            select t from Traitement t
            where t.conditionSanitaire.enfant.id = :enfantId
              and t.statut = :statut
              and t.dateDebut <= :date
              and (t.dateFin is null or t.dateFin >= :date)
            """)
    List<Traitement> findActifsValidesPourEnfantEtDate(@Param("enfantId") Long enfantId,
                                                      @Param("statut") StatutTraitement statut,
                                                      @Param("date") LocalDate date);
}
