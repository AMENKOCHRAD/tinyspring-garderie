package com.tinyspring.garderie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tinyspring.garderie.entity.Salle;

@Repository
public interface SalleRepository extends JpaRepository<Salle, Long> {
}
