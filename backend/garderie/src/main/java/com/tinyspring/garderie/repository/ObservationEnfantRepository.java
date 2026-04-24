package com.tinyspring.garderie.repository;

import com.tinyspring.garderie.entity.ObservationEnfant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ObservationEnfantRepository extends JpaRepository<ObservationEnfant, Long> {
    List<ObservationEnfant> findTop50ByEnfantIdOrderByCreeLeDesc(Long enfantId);

    List<ObservationEnfant> findTop50ByOrderByCreeLeDesc();

    List<ObservationEnfant> findTop5000ByOrderByCreeLeDesc();

    List<ObservationEnfant> findTop200ByEnfantParentEmailIgnoreCaseOrderByCreeLeDesc(String parentEmail);

    List<ObservationEnfant> findTop200ByEnfantParentEmailIgnoreCaseAndLuParentFalseOrderByCreeLeDesc(String parentEmail);

    long countByEnfantParentEmailIgnoreCaseAndLuParentFalse(String parentEmail);
}
