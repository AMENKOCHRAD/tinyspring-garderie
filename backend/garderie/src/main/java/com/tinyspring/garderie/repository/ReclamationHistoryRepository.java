package com.tinyspring.garderie.repository;

import com.tinyspring.garderie.entity.Reclamation;
import com.tinyspring.garderie.entity.ReclamationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReclamationHistoryRepository extends JpaRepository<ReclamationHistory, Long> {

    List<ReclamationHistory> findByReclamationOrderByCreatedAtDesc(Reclamation reclamation);
}