package com.tinyspring.garderie.repository;

import com.tinyspring.garderie.entity.Reclamation;
import com.tinyspring.garderie.entity.User;
import com.tinyspring.garderie.entity.enums.ReclamationCategory;
import com.tinyspring.garderie.entity.enums.ReclamationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

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
}