package com.tinyspring.garderie.service;

import com.tinyspring.garderie.dto.SendMessageRequest;
import com.tinyspring.garderie.dto.UpdateMessageReadStatusRequest;
import com.tinyspring.garderie.dto.UpdateMessageRequest;
import com.tinyspring.garderie.entity.Message;

import java.util.List;

public interface MessageService {

    Message sendMessage(Long conversationId, SendMessageRequest request);

    List<Message> getMessagesByConversation(Long conversationId);

    Message updateMessage(Long messageId, UpdateMessageRequest request);

    Message updateMessageReadStatus(Long messageId, UpdateMessageReadStatusRequest request);

    void deleteMessage(Long messageId);
}