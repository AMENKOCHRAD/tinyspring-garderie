package com.tinyspring.garderie.service.impl;

import com.tinyspring.garderie.dto.CreateConversationRequest;
import com.tinyspring.garderie.dto.UpdateConversationRequest;
import com.tinyspring.garderie.dto.UpdateConversationStatusRequest;
import com.tinyspring.garderie.entity.Conversation;
import com.tinyspring.garderie.entity.User;
import com.tinyspring.garderie.entity.enums.ConversationStatus;
import com.tinyspring.garderie.entity.enums.ConversationType;
import com.tinyspring.garderie.repository.ConversationRepository;
import com.tinyspring.garderie.repository.UserRepository;
import com.tinyspring.garderie.service.ConversationService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;

    public ConversationServiceImpl(ConversationRepository conversationRepository,
                                   UserRepository userRepository) {
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Conversation createConversation(CreateConversationRequest request) {
        User currentUser = getCurrentUser();

        if (currentUser.getRole() == null || currentUser.getRole().getName() == null) {
            throw new RuntimeException("Rôle utilisateur introuvable");
        }

        if (!currentUser.getRole().getName().name().equals("PARENT")) {
            throw new RuntimeException("Seul un parent peut créer une conversation");
        }

        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new RuntimeException("Destinataire introuvable"));

        if (request.getReceiverRole() == null || request.getReceiverRole().isBlank()) {
            throw new RuntimeException("receiverRole est obligatoire");
        }

        if (request.getSubject() == null || request.getSubject().isBlank()) {
            throw new RuntimeException("Le sujet est obligatoire");
        }

        String receiverRole = request.getReceiverRole().trim().toUpperCase();

        Conversation conversation = new Conversation();
        conversation.setSubject(request.getSubject().trim());
        conversation.setType(ConversationType.NORMAL);
        conversation.setStatus(ConversationStatus.OPEN);
        conversation.setCreatedBy(currentUser);
        conversation.setParent(currentUser);

        if (receiverRole.equals("ADMIN")) {
            if (!receiver.getRole().getName().name().equals("ADMIN")) {
                throw new RuntimeException("Le destinataire sélectionné n'est pas un ADMIN");
            }
            conversation.setAdmin(receiver);
            conversation.setAnimatrice(null);

        } else if (receiverRole.equals("ANIMATRICE")) {
            if (!receiver.getRole().getName().name().equals("ANIMATRICE")) {
                throw new RuntimeException("Le destinataire sélectionné n'est pas une ANIMATRICE");
            }
            conversation.setAnimatrice(receiver);
            conversation.setAdmin(null);

        } else {
            throw new RuntimeException("receiverRole doit être ADMIN ou ANIMATRICE");
        }

        return conversationRepository.save(conversation);
    }

    @Override
    public List<Conversation> getMyConversations() {
        User currentUser = getCurrentUser();

        String roleName = currentUser.getRole().getName().name();

        if (roleName.equals("PARENT")) {
            return conversationRepository.findByParent(currentUser);
        }

        if (roleName.equals("ADMIN")) {
            return conversationRepository.findByAdmin(currentUser);
        }

        if (roleName.equals("ANIMATRICE")) {
            return conversationRepository.findByAnimatrice(currentUser);
        }

        return new ArrayList<>();
    }

    @Override
    public Conversation getConversationById(Long id) {
        User currentUser = getCurrentUser();

        Conversation conversation = conversationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Conversation introuvable"));

        if (!isParticipant(conversation, currentUser)) {
            throw new RuntimeException("Accès refusé à cette conversation");
        }

        return conversation;
    }

    @Override
    public Conversation updateConversation(Long id, UpdateConversationRequest request) {
        User currentUser = getCurrentUser();

        Conversation conversation = conversationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Conversation introuvable"));

        if (!isParticipant(conversation, currentUser)) {
            throw new RuntimeException("Accès refusé à cette conversation");
        }

        if (request.getSubject() == null || request.getSubject().trim().isEmpty()) {
            throw new RuntimeException("Le sujet est obligatoire");
        }

        conversation.setSubject(request.getSubject().trim());

        return conversationRepository.save(conversation);
    }

    @Override
    public Conversation updateConversationStatus(Long id, UpdateConversationStatusRequest request) {
        User currentUser = getCurrentUser();

        Conversation conversation = conversationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Conversation introuvable"));

        if (!isParticipant(conversation, currentUser)) {
            throw new RuntimeException("Accès refusé à cette conversation");
        }

        if (request.getStatus() == null || request.getStatus().trim().isEmpty()) {
            throw new RuntimeException("Le statut est obligatoire");
        }

        try {
            conversation.setStatus(
                    ConversationStatus.valueOf(request.getStatus().trim().toUpperCase())
            );
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Statut invalide. Valeurs autorisées : OPEN, CLOSED, ARCHIVED");
        }

        return conversationRepository.save(conversation);
    }

    @Override
    public void deleteConversation(Long id) {
        User currentUser = getCurrentUser();

        Conversation conversation = conversationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Conversation introuvable"));

        if (!isParticipant(conversation, currentUser)) {
            throw new RuntimeException("Accès refusé à cette conversation");
        }

        conversationRepository.delete(conversation);
    }

    private boolean isParticipant(Conversation conversation, User user) {
        return (conversation.getParent() != null && conversation.getParent().getId().equals(user.getId()))
                || (conversation.getAdmin() != null && conversation.getAdmin().getId().equals(user.getId()))
                || (conversation.getAnimatrice() != null && conversation.getAnimatrice().getId().equals(user.getId()));
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur connecté introuvable"));
    }
}