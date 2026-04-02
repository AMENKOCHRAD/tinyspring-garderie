package com.tinyspring.garderie.controller;

import com.tinyspring.garderie.dto.SendMessageRequest;
import com.tinyspring.garderie.dto.UpdateMessageReadStatusRequest;
import com.tinyspring.garderie.dto.UpdateMessageRequest;
import com.tinyspring.garderie.entity.Message;
import com.tinyspring.garderie.service.MessageService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping("/api/conversations/{conversationId}/messages")
    public Message sendMessage(@PathVariable Long conversationId,
                               @RequestBody SendMessageRequest request) {
        return messageService.sendMessage(conversationId, request);
    }

    @GetMapping("/api/conversations/{conversationId}/messages")
    public List<Message> getMessages(@PathVariable Long conversationId) {
        return messageService.getMessagesByConversation(conversationId);
    }

    @PutMapping("/api/messages/{messageId}")
    public Message updateMessage(@PathVariable Long messageId,
                                 @RequestBody UpdateMessageRequest request) {
        return messageService.updateMessage(messageId, request);
    }

    @PutMapping("/api/messages/{messageId}/read")
    public Message updateMessageReadStatus(@PathVariable Long messageId,
                                           @RequestBody UpdateMessageReadStatusRequest request) {
        return messageService.updateMessageReadStatus(messageId, request);
    }

    @DeleteMapping("/api/messages/{messageId}")
    public void deleteMessage(@PathVariable Long messageId) {
        messageService.deleteMessage(messageId);
    }
}