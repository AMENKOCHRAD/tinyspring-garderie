package com.tinyspring.garderie.service.impl;

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
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

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
    public Message sendMessage(Long conversationId, String content, MultipartFile image) {
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

        boolean hasText = content != null && !content.trim().isEmpty();
        boolean hasImage = image != null && !image.isEmpty();

        if (!hasText && !hasImage) {
            throw new RuntimeException("Le message doit contenir un texte ou une image");
        }

        Message message = new Message();
        message.setContent(hasText ? content.trim() : null);
        message.setSender(currentUser);
        message.setConversation(conversation);
        message.setIsRead(false);

        if (hasImage) {
            try {
                String originalFilename = image.getOriginalFilename();
                String safeOriginalFilename = (originalFilename != null && !originalFilename.isBlank())
                        ? originalFilename.replaceAll("\\s+", "_")
                        : "image";

                String extension = "";
                int dotIndex = safeOriginalFilename.lastIndexOf(".");
                if (dotIndex != -1) {
                    extension = safeOriginalFilename.substring(dotIndex);
                }

                String uniqueFileName = UUID.randomUUID() + extension;

                java.nio.file.Path uploadPath = java.nio.file.Paths.get("uploads", "messages").toAbsolutePath().normalize();
                java.nio.file.Files.createDirectories(uploadPath);

                java.nio.file.Path targetPath = uploadPath.resolve(uniqueFileName);

                System.out.println("Upload dir = " + uploadPath);
                System.out.println("Target file = " + targetPath);

                java.nio.file.Files.copy(
                        image.getInputStream(),
                        targetPath,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING
                );

                message.setImageName(safeOriginalFilename);
                message.setImagePath("/uploads/messages/" + uniqueFileName);

            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("Erreur upload image : " + e.getMessage(), e);
            }
        }

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
    public void markConversationMessagesAsRead(Long conversationId) {
        User currentUser = getCurrentUser();

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation introuvable"));

        if (!isParticipant(conversation, currentUser)) {
            throw new RuntimeException("Accès refusé à cette conversation");
        }

        List<Message> messages = messageRepository.findByConversationOrderBySentAtAsc(conversation);

        for (Message message : messages) {
            boolean isReceivedMessage = message.getSender() != null
                    && !message.getSender().getId().equals(currentUser.getId());

            if (isReceivedMessage && Boolean.FALSE.equals(message.getIsRead())) {
                message.setIsRead(true);
                messageRepository.save(message);
            }
        }
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