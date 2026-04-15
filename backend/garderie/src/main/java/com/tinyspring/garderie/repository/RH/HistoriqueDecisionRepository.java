package com.tinyspring.garderie.repository.RH;

import com.tinyspring.garderie.entity.RH.HistoriqueDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistoriqueDecisionRepository extends JpaRepository<HistoriqueDecision, Long> {
    List<HistoriqueDecision> findByAbsenceCongeIdOrderByDateDecisionDesc(Long absenceId);
}