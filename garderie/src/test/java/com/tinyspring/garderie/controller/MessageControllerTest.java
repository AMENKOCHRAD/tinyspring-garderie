package com.tinyspring.garderie.controller;

import com.tinyspring.garderie.dto.UpdateMessageReadStatusRequest;
import com.tinyspring.garderie.dto.UpdateMessageRequest;
import com.tinyspring.garderie.entity.Message;
import com.tinyspring.garderie.service.MessageService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessageControllerTest {

    private final MessageService service = mock(MessageService.class);
    private final MessageController controller = new MessageController(service);

    @Test
    void messageEndpointsDelegateToService() {
        Message message = new Message();
        UpdateMessageRequest updateRequest = new UpdateMessageRequest();
        UpdateMessageReadStatusRequest readStatusRequest = new UpdateMessageReadStatusRequest();

        when(service.sendMessage(3L, "bonjour", null)).thenReturn(message);
        when(service.getMessagesByConversation(3L)).thenReturn(List.of(message));
        when(service.updateMessage(8L, updateRequest)).thenReturn(message);
        when(service.updateMessageReadStatus(8L, readStatusRequest)).thenReturn(message);

        assertSame(message, controller.sendMessage(3L, "bonjour", null));
        assertEquals(1, controller.getMessages(3L).size());
        assertSame(message, controller.updateMessage(8L, updateRequest));
        assertSame(message, controller.updateMessageReadStatus(8L, readStatusRequest));

        controller.markConversationMessagesAsRead(3L);
        controller.deleteMessage(8L);

        verify(service).markConversationMessagesAsRead(3L);
        verify(service).deleteMessage(8L);
    }
}
