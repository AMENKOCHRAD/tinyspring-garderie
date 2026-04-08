package com.tinyspring.garderie.controller;

import com.tinyspring.garderie.dto.UpdateMessageReadStatusRequest;
import com.tinyspring.garderie.dto.UpdateMessageRequest;
import com.tinyspring.garderie.entity.Message;
import com.tinyspring.garderie.service.MessageService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping(value = "/api/conversations/{conversationId}/messages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Message sendMessage(@PathVariable Long conversationId,
                               @RequestParam(value = "content", required = false) String content,
                               @RequestParam(value = "image", required = false) MultipartFile image) {
        return messageService.sendMessage(conversationId, content, image);
    }
    @PutMapping("/api/conversations/{conversationId}/messages/read")
    public void markConversationMessagesAsRead(@PathVariable Long conversationId) {
        messageService.markConversationMessagesAsRead(conversationId);
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