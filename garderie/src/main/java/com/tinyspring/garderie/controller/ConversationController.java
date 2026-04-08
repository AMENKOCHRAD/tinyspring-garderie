package com.tinyspring.garderie.controller;

import com.tinyspring.garderie.dto.CreateConversationRequest;
import com.tinyspring.garderie.dto.UpdateConversationRequest;
import com.tinyspring.garderie.dto.UpdateConversationStatusRequest;
import com.tinyspring.garderie.entity.Conversation;
import com.tinyspring.garderie.service.ConversationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping
    public Conversation createConversation(@RequestBody CreateConversationRequest request) {
        return conversationService.createConversation(request);
    }

    @GetMapping
    public List<Conversation> getMyConversations() {
        return conversationService.getMyConversations();
    }

    @GetMapping("/{id}")
    public Conversation getConversation(@PathVariable Long id) {
        return conversationService.getConversationById(id);
    }

    @PutMapping("/{id}")
    public Conversation updateConversation(@PathVariable Long id,
                                           @RequestBody UpdateConversationRequest request) {
        return conversationService.updateConversation(id, request);
    }

    @PutMapping("/{id}/status")
    public Conversation updateConversationStatus(@PathVariable Long id,
                                                 @RequestBody UpdateConversationStatusRequest request) {
        return conversationService.updateConversationStatus(id, request);
    }

    @DeleteMapping("/{id}")
    public void deleteConversation(@PathVariable Long id) {
        conversationService.deleteConversation(id);
    }
}