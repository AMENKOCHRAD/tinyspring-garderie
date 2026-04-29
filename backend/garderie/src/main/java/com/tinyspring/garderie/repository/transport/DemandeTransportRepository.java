package com.tinyspring.garderie.repository.transport;

import com.tinyspring.garderie.entity.transport.DemandeTransport;
import com.tinyspring.garderie.entity.transport.StatutDemandeTransport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DemandeTransportRepository extends JpaRepository<DemandeTransport, Long> {
    List<DemandeTransport> findAllByOrderByIdDesc();
    List<DemandeTransport> findByParentIdOrderByIdDesc(Long parentId);
    List<DemandeTransport> findTop10ByEnfantIdOrderByIdDesc(Long enfantId);
    boolean existsByEnfantIdAndStatut(Long enfantId, StatutDemandeTransport statut);
    boolean existsByTrajetId(Long trajetId);

    @Query("""
            SELECT d
            FROM DemandeTransport d
            WHERE d.statut = :statut
            ORDER BY d.dateDemande ASC, d.id ASC
            """)
    List<DemandeTransport> findDemandesByStatutOrderByOldestFirst(@Param("statut") StatutDemandeTransport statut);

    @Query("""
            SELECT COUNT(d)
            FROM DemandeTransport d
            WHERE d.statut = :statut
            AND d.suspicious = true
            """)
    long countSuspiciousDemandesByStatut(@Param("statut") StatutDemandeTransport statut);
}
