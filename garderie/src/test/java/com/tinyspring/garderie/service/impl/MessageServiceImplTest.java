package com.tinyspring.garderie.service.impl;

import com.tinyspring.garderie.dto.UpdateMessageReadStatusRequest;
import com.tinyspring.garderie.dto.UpdateMessageRequest;
import com.tinyspring.garderie.entity.Conversation;
import com.tinyspring.garderie.entity.Message;
import com.tinyspring.garderie.entity.Role;
import com.tinyspring.garderie.entity.RoleName;
import com.tinyspring.garderie.entity.User;
import com.tinyspring.garderie.entity.enums.ConversationStatus;
import com.tinyspring.garderie.repository.ConversationRepository;
import com.tinyspring.garderie.repository.MessageRepository;
import com.tinyspring.garderie.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessageServiceImplTest {

    private MessageRepository messageRepository;
    private ConversationRepository conversationRepository;
    private UserRepository userRepository;
    private MessageServiceImpl service;

    private User parent;
    private User admin;
    private User animatrice;
    private User outsider;

    @BeforeEach
    void setUp() {
        messageRepository = mock(MessageRepository.class);
        conversationRepository = mock(ConversationRepository.class);
        userRepository = mock(UserRepository.class);
        service = new MessageServiceImpl(messageRepository, conversationRepository, userRepository);

        parent = user(1L, "parent@garderie.com", RoleName.PARENT);
        admin = user(2L, "admin@garderie.com", RoleName.ADMIN);
        animatrice = user(3L, "animatrice@garderie.com", RoleName.ANIMATRICE);
        outsider = user(4L, "other@garderie.com", RoleName.PARENT);

        when(userRepository.findByEmail(parent.getEmail())).thenReturn(Optional.of(parent));
        when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(userRepository.findByEmail(animatrice.getEmail())).thenReturn(Optional.of(animatrice));
        when(userRepository.findByEmail(outsider.getEmail())).thenReturn(Optional.of(outsider));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void sendMessageTrimsContentAndRejectsClosedEmptyOrUnauthorizedConversation() {
        authenticate(parent.getEmail());
        Conversation conversation = conversation(ConversationStatus.OPEN);
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(conversation));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Message saved = service.sendMessage(10L, "  Bonjour admin  ", null);

        assertEquals("Bonjour admin", saved.getContent());
        assertSame(parent, saved.getSender());
        assertSame(conversation, saved.getConversation());
        assertFalse(saved.getIsRead());

        assertThrows(RuntimeException.class, () -> service.sendMessage(10L, "   ", null));

        conversation.setStatus(ConversationStatus.CLOSED);
        assertThrows(RuntimeException.class, () -> service.sendMessage(10L, "Message", null));

        authenticate(outsider.getEmail());
        conversation.setStatus(ConversationStatus.OPEN);
        assertThrows(RuntimeException.class, () -> service.sendMessage(10L, "Message", null));
    }

    @Test
    void getMessagesRequiresParticipant() {
        Conversation conversation = conversation(ConversationStatus.OPEN);
        Message message = message(parent, conversation, false);
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversationOrderBySentAtAsc(conversation)).thenReturn(List.of(message));

        authenticate(admin.getEmail());
        assertEquals(1, service.getMessagesByConversation(10L).size());

        authenticate(outsider.getEmail());
        assertThrows(RuntimeException.class, () -> service.getMessagesByConversation(10L));
    }

    @Test
    void updateMessageAllowsOnlyAuthorAndRequiresContent() {
        authenticate(parent.getEmail());
        Conversation conversation = conversation(ConversationStatus.OPEN);
        Message message = message(parent, conversation, false);
        when(messageRepository.findById(5L)).thenReturn(Optional.of(message));
        when(messageRepository.save(message)).thenReturn(message);

        Message updated = service.updateMessage(5L, new UpdateMessageRequest("  Texte corrige  "));

        assertEquals("Texte corrige", updated.getContent());
        assertThrows(RuntimeException.class, () -> service.updateMessage(5L, new UpdateMessageRequest(" ")));

        authenticate(admin.getEmail());
        assertThrows(RuntimeException.class, () -> service.updateMessage(5L, new UpdateMessageRequest("Autre")));
    }

    @Test
    void updateReadStatusAndMarkConversationMessagesAsRead() {
        authenticate(parent.getEmail());
        Conversation conversation = conversation(ConversationStatus.OPEN);
        Message receivedUnread = message(admin, conversation, false);
        Message ownUnread = message(parent, conversation, false);
        Message receivedRead = message(animatrice, conversation, true);

        when(messageRepository.findById(5L)).thenReturn(Optional.of(receivedUnread));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversationOrderBySentAtAsc(conversation))
                .thenReturn(List.of(receivedUnread, ownUnread, receivedRead));

        Message marked = service.updateMessageReadStatus(5L, new UpdateMessageReadStatusRequest(true));
        assertTrue(marked.getIsRead());

        assertThrows(RuntimeException.class,
                () -> service.updateMessageReadStatus(5L, new UpdateMessageReadStatusRequest(null)));

        receivedUnread.setIsRead(false);
        service.markConversationMessagesAsRead(10L);

        assertTrue(receivedUnread.getIsRead());
        assertFalse(ownUnread.getIsRead());
        assertTrue(receivedRead.getIsRead());
        verify(messageRepository, times(2)).save(receivedUnread);
    }

    @Test
    void deleteMessageAllowsOnlyAuthor() {
        Conversation conversation = conversation(ConversationStatus.OPEN);
        Message message = message(parent, conversation, false);
        when(messageRepository.findById(5L)).thenReturn(Optional.of(message));

        authenticate(admin.getEmail());
        assertThrows(RuntimeException.class, () -> service.deleteMessage(5L));
        verify(messageRepository, never()).delete(message);

        authenticate(parent.getEmail());
        service.deleteMessage(5L);
        verify(messageRepository).delete(message);
    }

    @Test
    void messageWithNoSenderOrConversationIsRejectedWhereNeeded() {
        authenticate(parent.getEmail());
        Message message = new Message();
        ReflectionTestUtils.setField(message, "id", 5L);
        message.setContent("Ancien");
        when(messageRepository.findById(5L)).thenReturn(Optional.of(message));

        assertThrows(RuntimeException.class, () -> service.updateMessage(5L, new UpdateMessageRequest("Nouveau")));
        assertThrows(RuntimeException.class,
                () -> service.updateMessageReadStatus(5L, new UpdateMessageReadStatusRequest(true)));
        assertThrows(RuntimeException.class, () -> service.deleteMessage(5L));
        assertNull(message.getConversation());
    }

    private Conversation conversation(ConversationStatus status) {
        Conversation conversation = new Conversation();
        ReflectionTestUtils.setField(conversation, "id", 10L);
        conversation.setSubject("Question");
        conversation.setParent(parent);
        conversation.setAdmin(admin);
        conversation.setAnimatrice(animatrice);
        conversation.setStatus(status);
        return conversation;
    }

    private Message message(User sender, Conversation conversation, Boolean isRead) {
        Message message = new Message();
        ReflectionTestUtils.setField(message, "id", 5L);
        message.setContent("Message");
        message.setSender(sender);
        message.setConversation(conversation);
        message.setIsRead(isRead);
        return message;
    }

    private User user(Long id, String email, RoleName roleName) {
        User user = new User("User", email, "password", true, new Role(roleName));
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private void authenticate(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, "password")
        );
    }
}
