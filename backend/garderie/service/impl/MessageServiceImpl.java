package com.tinyspring.garderie.service.impl;

import com.tinyspring.garderie.dto.SendMessageRequest;
import com.tinyspring.garderie.dto.UpdateMessageReadStatusRequest;
import com.tinyspring.garderie.dto.UpdateMessageRequest;
import com.tinyspring.garderie.entity.Conversation;
import com.tinyspring.garderie.entity.Message;
import com.tinyspring.garderie.entity.User;
import com.tinyspring.garderie.entity.enums.ConversationStatus;
import com.tinyspring.garderie.repository.ConversationRepository;
import com.tinyspring.garderie.repository.MessageRepository;
import com.tinyspring.garderie.repository.UserRepository;
import com.tinyspring.garderie.service.MessageService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;

    public MessageServiceImpl(MessageRepository messageRepository,
                              ConversationRepository conversationRepository,
                              UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Message sendMessage(Long conversationId, SendMessageRequest request) {
        User currentUser = getCurrentUser();

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation introuvable"));

        if (!isParticipant(conversation, currentUser)) {
            throw new RuntimeException("Accès refusé : vous n'êtes pas participant à cette conversation");
        }

        if (conversation.getStatus() == ConversationStatus.CLOSED ||
                conversation.getStatus() == ConversationStatus.ARCHIVED) {
            throw new RuntimeException("Impossible d'envoyer un message dans une conversation fermée ou archivée");
        }

        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new RuntimeException("Le contenu du message est obligatoire");
        }

        Message message = new Message();
        message.setContent(request.getContent().trim());
        message.setSender(currentUser);
        message.setConversation(conversation);
        message.setIsRead(false);

        return messageRepository.save(message);
    }

    @Override
    public List<Message> getMessagesByConversation(Long conversationId) {
        User currentUser = getCurrentUser();

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation introuvable"));

        if (!isParticipant(conversation, currentUser)) {
            throw new RuntimeException("Accès refusé à cette conversation");
        }

        return messageRepository.findByConversationOrderBySentAtAsc(conversation);
    }

    @Override
    public Message updateMessage(Long messageId, UpdateMessageRequest request) {
        User currentUser = getCurrentUser();

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message introuvable"));

        if (message.getSender() == null || !message.getSender().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Seul l'auteur du message peut le modifier");
        }

        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new RuntimeException("Le contenu du message est obligatoire");
        }

        message.setContent(request.getContent().trim());

        return messageRepository.save(message);
    }

    @Override
    public Message updateMessageReadStatus(Long messageId, UpdateMessageReadStatusRequest request) {
        User currentUser = getCurrentUser();

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message introuvable"));

        Conversation conversation = message.getConversation();

        if (conversation == null || !isParticipant(conversation, currentUser)) {
            throw new RuntimeException("Accès refusé");
        }

        if (request.getIsRead() == null) {
            throw new RuntimeException("Le statut de lecture est obligatoire");
        }

        message.setIsRead(request.getIsRead());

        return messageRepository.save(message);
    }

    @Override
    public void deleteMessage(Long messageId) {
        User currentUser = getCurrentUser();

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message introuvable"));

        if (message.getSender() == null || !message.getSender().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Seul l'auteur du message peut supprimer ce message");
        }

        messageRepository.delete(message);
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