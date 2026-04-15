package com.tinyspring.garderie.repository.RH;

import com.tinyspring.garderie.entity.RH.QuotaConge;
import com.tinyspring.garderie.entity.RH.enums.TypeAbsenceConge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuotaCongeRepository extends JpaRepository<QuotaConge, Long> {
    Optional<QuotaConge> findByType(TypeAbsenceConge type);
}