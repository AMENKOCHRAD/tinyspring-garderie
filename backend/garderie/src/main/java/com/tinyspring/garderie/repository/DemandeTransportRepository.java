package com.tinyspring.garderie.repository;

import com.tinyspring.garderie.entity.DemandeTransport;
import com.tinyspring.garderie.entity.StatutDemandeTransport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DemandeTransportRepository extends JpaRepository<DemandeTransport, Long> {
    List<DemandeTransport> findAllByOrderByIdDesc();
    List<DemandeTransport> findByParentIdOrderByIdDesc(Long parentId);
    boolean existsByEnfantIdAndStatut(Long enfantId, StatutDemandeTransport statut);
    boolean existsByTrajetId(Long trajetId);
}
