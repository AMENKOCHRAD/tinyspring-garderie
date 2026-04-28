package com.tinyspring.garderie.repository;

import com.tinyspring.garderie.entity.Reclamation;
import com.tinyspring.garderie.entity.User;
import com.tinyspring.garderie.entity.enums.ReclamationCategory;
import com.tinyspring.garderie.entity.enums.ReclamationStatus;
import com.tinyspring.garderie.entity.enums.SmartPriorityLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReclamationRepository extends JpaRepository<Reclamation, Long> {

    List<Reclamation> findByParent(User parent);

    List<Reclamation> findByAssignedAdmin(User assignedAdmin);

    Optional<Reclamation> findByConversationId(Long conversationId);

    List<Reclamation> findByCategoryAndCreatedAtAfterAndStatusIn(
            ReclamationCategory category,
            LocalDateTime createdAt,
            Collection<ReclamationStatus> statuses
    );

    // ── Requêtes pour le Dashboard Admin intelligent ──────────────────────

    /**
     * Réclamations actives (OPEN + IN_PROGRESS) triées par smartPriorityScore DESC.
     * C'est la requête centrale du dashboard : les plus urgentes en premier.
     */
    @Query("SELECT r FROM Reclamation r WHERE r.status IN :statuses " +
           "ORDER BY r.smartPriorityScore DESC, r.createdAt ASC")
    List<Reclamation> findActiveReclamationsSortedBySmartPriority(
            @Param("statuses") Collection<ReclamationStatus> statuses
    );

    /**
     * Compte les réclamations par statut pour les KPIs du dashboard.
     */
    long countByStatus(ReclamationStatus status);

    /**
     * Compte les réclamations actives par niveau SmartPriority.
     */
    long countByStatusInAndSmartPriorityLevel(
            Collection<ReclamationStatus> statuses,
            SmartPriorityLevel level
    );

    /**
     * Compte les réclamations actives sans admin assigné.
     */
    @Query("SELECT COUNT(r) FROM Reclamation r WHERE r.status IN :statuses " +
           "AND r.assignedAdmin IS NULL")
    long countUnassignedActive(@Param("statuses") Collection<ReclamationStatus> statuses);

    /**
     * Compte les réclamations actives récurrentes.
     */
    @Query("SELECT COUNT(r) FROM Reclamation r WHERE r.status IN :statuses " +
           "AND r.recurring = true")
    long countRecurringActive(@Param("statuses") Collection<ReclamationStatus> statuses);

    /**
     * Score moyen des réclamations actives.
     */
    @Query("SELECT COALESCE(AVG(r.smartPriorityScore), 0.0) FROM Reclamation r " +
           "WHERE r.status IN :statuses")
    double averageSmartPriorityScore(@Param("statuses") Collection<ReclamationStatus> statuses);

    /**
     * Score maximum parmi les réclamations actives.
     */
    @Query("SELECT COALESCE(MAX(r.smartPriorityScore), 0) FROM Reclamation r " +
           "WHERE r.status IN :statuses")
    int maxSmartPriorityScore(@Param("statuses") Collection<ReclamationStatus> statuses);

    /**
     * Compte les réclamations actives dont le SLA est dépassé (smartPriorityLevel = CRITICAL).
     * On se base sur le smartPriorityScore >= 30 (score SLA max = dépassé).
     */
    @Query("SELECT COUNT(r) FROM Reclamation r WHERE r.status IN :statuses " +
           "AND r.smartPriorityScore >= :minScore")
    long countActiveWithScoreGreaterOrEqual(
            @Param("statuses") Collection<ReclamationStatus> statuses,
            @Param("minScore") int minScore
    );
}