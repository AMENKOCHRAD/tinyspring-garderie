package com.tinyspring.garderie.repository.RH;

import com.tinyspring.garderie.entity.RH.AbsenceConge;
import com.tinyspring.garderie.entity.RH.enums.StatutAbsenceConge;
import com.tinyspring.garderie.entity.RH.enums.TypeAbsenceConge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AbsenceCongeRepository extends JpaRepository<AbsenceConge, Long> {

    List<AbsenceConge> findByAnimatriceId(Long animatriceId);

    List<AbsenceConge> findByStatut(StatutAbsenceConge statut);

    List<AbsenceConge> findByType(TypeAbsenceConge type);

    List<AbsenceConge> findByAnimatriceIdAndStatut(Long animatriceId, StatutAbsenceConge statut);
}