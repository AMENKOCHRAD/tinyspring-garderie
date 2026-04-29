package com.tinyspring.garderie.service.impl;

import com.tinyspring.garderie.dto.CreateConversationRequest;
import com.tinyspring.garderie.dto.UpdateConversationRequest;
import com.tinyspring.garderie.dto.UpdateConversationStatusRequest;
import com.tinyspring.garderie.entity.Conversation;
import com.tinyspring.garderie.entity.Role;
import com.tinyspring.garderie.entity.RoleName;
import com.tinyspring.garderie.entity.User;
import com.tinyspring.garderie.entity.enums.ConversationStatus;
import com.tinyspring.garderie.entity.enums.ConversationType;
import com.tinyspring.garderie.repository.ConversationRepository;
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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConversationServiceImplTest {

    private ConversationRepository conversationRepository;
    private UserRepository userRepository;
    private ConversationServiceImpl service;

    private User parent;
    private User admin;
    private User animatrice;
    private User outsider;

    @BeforeEach
    void setUp() {
        conversationRepository = mock(ConversationRepository.class);
        userRepository = mock(UserRepository.class);
        service = new ConversationServiceImpl(conversationRepository, userRepository);

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
    void createConversationAllowsParentToContactAdminOrAnimatrice() {
        authenticate(parent.getEmail());
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(userRepository.findById(3L)).thenReturn(Optional.of(animatrice));
        when(conversationRepository.save(org.mockito.ArgumentMatchers.any(Conversation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Conversation toAdmin = service.createConversation(new CreateConversationRequest("  Paiement  ", 2L, "admin"));
        Conversation toAnimatrice = service.createConversation(new CreateConversationRequest("Activite", 3L, "ANIMATRICE"));

        assertEquals("Paiement", toAdmin.getSubject());
        assertEquals(ConversationType.NORMAL, toAdmin.getType());
        assertEquals(ConversationStatus.OPEN, toAdmin.getStatus());
        assertSame(parent, toAdmin.getParent());
        assertSame(admin, toAdmin.getAdmin());
        assertSame(animatrice, toAnimatrice.getAnimatrice());
    }

    @Test
    void createConversationRejectsInvalidActorAndReceiverData() {
        authenticate(admin.getEmail());
        RuntimeException notParent = assertThrows(RuntimeException.class,
                () -> service.createConversation(new CreateConversationRequest("Sujet", 1L, "ADMIN")));
        assertTrue(notParent.getMessage().contains("parent"));

        authenticate(parent.getEmail());
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));

        assertThrows(RuntimeException.class,
                () -> service.createConversation(new CreateConversationRequest("Sujet", 2L, "ANIMATRICE")));
        assertThrows(RuntimeException.class,
                () -> service.createConversation(new CreateConversationRequest("Sujet", 2L, "RESPONSABLE")));
        assertThrows(RuntimeException.class,
                () -> service.createConversation(new CreateConversationRequest(" ", 2L, "ADMIN")));
    }

    @Test
    void getMyConversationsUsesRoleSpecificRepositoryMethod() {
        Conversation conversation = conversation();

        authenticate(parent.getEmail());
        when(conversationRepository.findByParent(parent)).thenReturn(List.of(conversation));
        assertEquals(1, service.getMyConversations().size());

        authenticate(admin.getEmail());
        when(conversationRepository.findByAdmin(admin)).thenReturn(List.of(conversation));
        assertEquals(1, service.getMyConversations().size());

        authenticate(animatrice.getEmail());
        when(conversationRepository.findByAnimatrice(animatrice)).thenReturn(List.of(conversation));
        assertEquals(1, service.getMyConversations().size());
    }

    @Test
    void participantCanReadUpdateStatusAndDeleteConversation() {
        authenticate(parent.getEmail());
        Conversation conversation = conversation();
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(conversation));
        when(conversationRepository.save(conversation)).thenReturn(conversation);

        assertSame(conversation, service.getConversationById(10L));

        Conversation renamed = service.updateConversation(10L, new UpdateConversationRequest("  Nouveau sujet  "));
        assertEquals("Nouveau sujet", renamed.getSubject());

        Conversation closed = service.updateConversationStatus(10L, new UpdateConversationStatusRequest("closed"));
        assertEquals(ConversationStatus.CLOSED, closed.getStatus());

        service.deleteConversation(10L);
        verify(conversationRepository).delete(conversation);
    }

    @Test
    void nonParticipantAndInvalidUpdatesAreRejected() {
        authenticate(outsider.getEmail());
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(conversation()));

        assertThrows(RuntimeException.class, () -> service.getConversationById(10L));
        assertThrows(RuntimeException.class, () -> service.updateConversation(10L, new UpdateConversationRequest("Sujet")));

        authenticate(parent.getEmail());
        assertThrows(RuntimeException.class, () -> service.updateConversation(10L, new UpdateConversationRequest(" ")));
        assertThrows(RuntimeException.class, () -> service.updateConversationStatus(10L, new UpdateConversationStatusRequest("BAD")));
    }

    private Conversation conversation() {
        Conversation conversation = new Conversation();
        ReflectionTestUtils.setField(conversation, "id", 10L);
        conversation.setSubject("Question");
        conversation.setParent(parent);
        conversation.setAdmin(admin);
        conversation.setStatus(ConversationStatus.OPEN);
        conversation.setType(ConversationType.NORMAL);
        return conversation;
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
