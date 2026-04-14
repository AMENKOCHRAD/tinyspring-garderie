package com.tinyspring.garderie.repository.transport;

import com.tinyspring.garderie.entity.transport.DemandeTransport;
import com.tinyspring.garderie.entity.transport.StatutDemandeTransport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DemandeTransportRepository extends JpaRepository<DemandeTransport, Long> {
    List<DemandeTransport> findAllByOrderByIdDesc();
    List<DemandeTransport> findByParentIdOrderByIdDesc(Long parentId);
    List<DemandeTransport> findTop10ByEnfantIdOrderByIdDesc(Long enfantId);
    boolean existsByEnfantIdAndStatut(Long enfantId, StatutDemandeTransport statut);
    boolean existsByTrajetId(Long trajetId);
}
