package com.tinyspring.garderie.repository;

import com.tinyspring.garderie.entity.ConditionSanitaire;
import com.tinyspring.garderie.entity.TypeConditionSanitaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ConditionSanitaireRepository extends JpaRepository<ConditionSanitaire, Long> {
    List<ConditionSanitaire> findByEnfantId(Long enfantId);

    @Query("""
            select c from ConditionSanitaire c
            where c.enfant.id = :enfantId
              and c.type = :type
              and (c.dateDebut is null or c.dateDebut <= :date)
              and (c.dateFin is null or c.dateFin >= :date)
            """)
    List<ConditionSanitaire> findActivesPourEnfantEtType(@Param("enfantId") Long enfantId,
                                                        @Param("type") TypeConditionSanitaire type,
                                                        @Param("date") LocalDate date);

    default List<ConditionSanitaire> findChroniquesActivesPourEnfant(Long enfantId, LocalDate date) {
        return findActivesPourEnfantEtType(enfantId, TypeConditionSanitaire.MALADIE_CHRONIQUE, date);
    }
}
