package com.tinyspring.garderie.repository.transport;

import com.tinyspring.garderie.entity.transport.AffectationTransport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AffectationTransportRepository extends JpaRepository<AffectationTransport, Long> {
    boolean existsByEnfantId(Long enfantId);
    long countByTransportId(Long transportId);
    List<AffectationTransport> findByTrajetId(Long trajetId);
    Optional<AffectationTransport> findByEnfantId(Long enfantId);
}
