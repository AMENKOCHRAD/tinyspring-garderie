package com.tinyspring.garderie.repository;

import com.tinyspring.garderie.entity.Message;
import com.tinyspring.garderie.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationOrderBySentAtAsc(Conversation conversation);
}