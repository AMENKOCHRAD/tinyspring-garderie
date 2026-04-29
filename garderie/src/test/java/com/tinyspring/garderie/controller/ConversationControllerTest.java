package com.tinyspring.garderie.controller;

import com.tinyspring.garderie.dto.CreateConversationRequest;
import com.tinyspring.garderie.dto.UpdateConversationRequest;
import com.tinyspring.garderie.dto.UpdateConversationStatusRequest;
import com.tinyspring.garderie.entity.Conversation;
import com.tinyspring.garderie.service.ConversationService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConversationControllerTest {

    private final ConversationService service = mock(ConversationService.class);
    private final ConversationController controller = new ConversationController(service);

    @Test
    void conversationEndpointsDelegateToService() {
        Conversation conversation = new Conversation();
        CreateConversationRequest createRequest = new CreateConversationRequest();
        UpdateConversationRequest updateRequest = new UpdateConversationRequest();
        UpdateConversationStatusRequest statusRequest = new UpdateConversationStatusRequest();

        when(service.createConversation(createRequest)).thenReturn(conversation);
        when(service.getMyConversations()).thenReturn(List.of(conversation));
        when(service.getConversationById(2L)).thenReturn(conversation);
        when(service.updateConversation(2L, updateRequest)).thenReturn(conversation);
        when(service.updateConversationStatus(2L, statusRequest)).thenReturn(conversation);

        assertSame(conversation, controller.createConversation(createRequest));
        assertEquals(1, controller.getMyConversations().size());
        assertSame(conversation, controller.getConversation(2L));
        assertSame(conversation, controller.updateConversation(2L, updateRequest));
        assertSame(conversation, controller.updateConversationStatus(2L, statusRequest));

        controller.deleteConversation(2L);
        verify(service).deleteConversation(2L);
    }
}
