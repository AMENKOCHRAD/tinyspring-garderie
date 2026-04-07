package com.tinyspring.garderie.repository;

import com.tinyspring.garderie.entity.ConditionSanitaire;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConditionSanitaireRepository extends JpaRepository<ConditionSanitaire, Long> {
    List<ConditionSanitaire> findByEnfantId(Long enfantId);
}