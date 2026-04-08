package com.tinyspring.garderie.repository;

import com.tinyspring.garderie.entity.Conversation;
import com.tinyspring.garderie.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    List<Conversation> findByParent(User parent);

    List<Conversation> findByAdmin(User admin);

    List<Conversation> findByAnimatrice(User animatrice);

    List<Conversation> findByCreatedBy(User createdBy);
}