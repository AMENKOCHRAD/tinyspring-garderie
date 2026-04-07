package com.tinyspring.garderie.service;

import com.tinyspring.garderie.dto.UpdateMessageReadStatusRequest;
import com.tinyspring.garderie.dto.UpdateMessageRequest;
import com.tinyspring.garderie.entity.Message;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MessageService {

    Message sendMessage(Long conversationId, String content, MultipartFile image);

    List<Message> getMessagesByConversation(Long conversationId);

    Message updateMessage(Long messageId, UpdateMessageRequest request);

    Message updateMessageReadStatus(Long messageId, UpdateMessageReadStatusRequest request);

    void markConversationMessagesAsRead(Long conversationId);

    void deleteMessage(Long messageId);
}