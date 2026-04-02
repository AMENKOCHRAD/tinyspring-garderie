package com.tinyspring.garderie.repository;

import com.tinyspring.garderie.entity.Reclamation;
import com.tinyspring.garderie.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReclamationRepository extends JpaRepository<Reclamation, Long> {

    List<Reclamation> findByParent(User parent);

    List<Reclamation> findByAssignedAdmin(User assignedAdmin);

    Optional<Reclamation> findByConversationId(Long conversationId);
}