package com.tinyspring.garderie.service;

import com.tinyspring.garderie.dto.CreateConversationRequest;
import com.tinyspring.garderie.dto.UpdateConversationRequest;
import com.tinyspring.garderie.dto.UpdateConversationStatusRequest;
import com.tinyspring.garderie.entity.Conversation;

import java.util.List;

public interface ConversationService {

    Conversation createConversation(CreateConversationRequest request);

    List<Conversation> getMyConversations();

    Conversation getConversationById(Long id);

    Conversation updateConversation(Long id, UpdateConversationRequest request);

    Conversation updateConversationStatus(Long id, UpdateConversationStatusRequest request);

    void deleteConversation(Long id);
}