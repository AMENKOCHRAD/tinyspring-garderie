package com.tinyspring.garderie.repository.transport;

import com.tinyspring.garderie.entity.transport.Trajet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TrajetRepository extends JpaRepository<Trajet, Long> {
    List<Trajet> findByDateTrajetGreaterThanEqualOrderByDateTrajetAscHeureDepartAsc(LocalDate date);
}
