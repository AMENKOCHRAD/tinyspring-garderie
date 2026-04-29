package com.tinyspring.garderie.repository.boutique;

import com.tinyspring.garderie.entity.boutique.UserInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserInteractionRepository extends JpaRepository<UserInteraction, Long> {
    List<UserInteraction> findByUserIdOrderByCreatedAtDesc(Long userId);
}