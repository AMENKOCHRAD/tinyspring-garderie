package com.tinyspring.garderie.repository.RH;

import com.tinyspring.garderie.entity.RH.HistoriqueDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistoriqueDecisionRepository extends JpaRepository<HistoriqueDecision, Long> {

    List<HistoriqueDecision> findByAbsenceCongeIdOrderByDateDecisionDesc(Long absenceId);

    // ✅ @Modifying + @Query — suppression SQL directe sans passer par Hibernate
    @Modifying
    @Query("DELETE FROM HistoriqueDecision hd WHERE hd.absenceConge.animatrice.id = :animatriceId")
    void deleteByAbsenceCongeAnimatriceId(@Param("animatriceId") Long animatriceId);
}