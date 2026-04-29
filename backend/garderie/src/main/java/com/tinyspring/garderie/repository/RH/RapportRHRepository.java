package com.tinyspring.garderie.repository.RH;

import com.tinyspring.garderie.entity.RH.RapportRH;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RapportRHRepository extends JpaRepository<RapportRH, Long> {
    List<RapportRH> findAllByOrderByDateGenerationDesc();
}