package com.tinyspring.garderie.service.impl;

import com.tinyspring.garderie.dto.UpdateReclamationRequest;
import com.tinyspring.garderie.dto.UpdateReclamationStatusRequest;
import com.tinyspring.garderie.entity.Conversation;
import com.tinyspring.garderie.entity.Reclamation;
import com.tinyspring.garderie.entity.User;
import com.tinyspring.garderie.entity.enums.ConversationStatus;
import com.tinyspring.garderie.entity.enums.ConversationType;
import com.tinyspring.garderie.entity.enums.ReclamationCategory;
import com.tinyspring.garderie.entity.enums.ReclamationPriority;
import com.tinyspring.garderie.entity.enums.ReclamationStatus;
import com.tinyspring.garderie.repository.ConversationRepository;
import com.tinyspring.garderie.repository.ReclamationRepository;
import com.tinyspring.garderie.repository.UserRepository;
import com.tinyspring.garderie.service.ReclamationService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
public class ReclamationServiceImpl implements ReclamationService {

    private final ReclamationRepository reclamationRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;

    public ReclamationServiceImpl(ReclamationRepository reclamationRepository,
                                  ConversationRepository conversationRepository,
                                  UserRepository userRepository) {
        this.reclamationRepository = reclamationRepository;
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Reclamation createReclamation(String title,
                                         String description,
                                         String priority,
                                         String category,
                                         MultipartFile image,
                                         MultipartFile attachment) {
        User currentUser = getCurrentUser();

        String roleName = currentUser.getRole().getName().name();

        if (!roleName.equals("PARENT")) {
            throw new RuntimeException("Seul un parent peut créer une réclamation");
        }

        if (title == null || title.trim().isEmpty()) {
            throw new RuntimeException("Le titre est obligatoire");
        }

        if (description == null || description.trim().isEmpty()) {
            throw new RuntimeException("La description est obligatoire");
        }

        if (category == null || category.trim().isEmpty()) {
            throw new RuntimeException("La catégorie est obligatoire");
        }

        Conversation conversation = new Conversation();
        conversation.setSubject("Réclamation : " + title.trim());
        conversation.setType(ConversationType.RECLAMATION);
        conversation.setStatus(ConversationStatus.OPEN);
        conversation.setCreatedBy(currentUser);
        conversation.setParent(currentUser);

        Conversation savedConversation = conversationRepository.save(conversation);

        Reclamation reclamation = new Reclamation();
        reclamation.setTitle(title.trim());
        reclamation.setDescription(description.trim());
        reclamation.setParent(currentUser);
        reclamation.setConversation(savedConversation);
        reclamation.setStatus(ReclamationStatus.OPEN);

        try {
            reclamation.setCategory(
                    ReclamationCategory.valueOf(category.trim().toUpperCase())
            );
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Catégorie invalide. Valeurs autorisées : REPAS, TRANSPORT, COMPORTEMENT, HYGIENE, SECURITE, PERSONNEL, AUTRE");
        }

        if (priority != null && !priority.trim().isEmpty()) {
            try {
                reclamation.setPriority(
                        ReclamationPriority.valueOf(priority.trim().toUpperCase())
                );
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Priorité invalide. Valeurs autorisées : LOW, MEDIUM, HIGH");
            }
        } else {
            reclamation.setPriority(ReclamationPriority.MEDIUM);
        }

        boolean hasImage = image != null && !image.isEmpty();
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

                Path uploadPath = Paths.get("uploads", "reclamations").toAbsolutePath().normalize();
                Files.createDirectories(uploadPath);

                Path targetPath = uploadPath.resolve(uniqueFileName);

                Files.copy(
                        image.getInputStream(),
                        targetPath,
                        StandardCopyOption.REPLACE_EXISTING
                );

                reclamation.setImageName(safeOriginalFilename);
                reclamation.setImagePath("/uploads/reclamations/" + uniqueFileName);

            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("Erreur upload image réclamation : " + e.getMessage(), e);
            }
        }

        boolean hasAttachment = attachment != null && !attachment.isEmpty();
        if (hasAttachment) {
            try {
                String originalFilename = attachment.getOriginalFilename();
                String safeOriginalFilename = (originalFilename != null && !originalFilename.isBlank())
                        ? originalFilename.replaceAll("\\s+", "_")
                        : "attachment";

                String extension = "";
                int dotIndex = safeOriginalFilename.lastIndexOf(".");
                if (dotIndex != -1) {
                    extension = safeOriginalFilename.substring(dotIndex);
                }

                String uniqueFileName = UUID.randomUUID() + extension;

                Path uploadPath = Paths.get("uploads", "reclamations", "attachments").toAbsolutePath().normalize();
                Files.createDirectories(uploadPath);

                Path targetPath = uploadPath.resolve(uniqueFileName);

                Files.copy(
                        attachment.getInputStream(),
                        targetPath,
                        StandardCopyOption.REPLACE_EXISTING
                );

                reclamation.setAttachmentName(safeOriginalFilename);
                reclamation.setAttachmentPath("/uploads/reclamations/attachments/" + uniqueFileName);
                reclamation.setAttachmentType(attachment.getContentType());

            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("Erreur upload pièce jointe réclamation : " + e.getMessage(), e);
            }
        }

        return reclamationRepository.save(reclamation);
    }

    @Override
    public List<Reclamation> getMyReclamations() {
        User currentUser = getCurrentUser();

        String roleName = currentUser.getRole().getName().name();

        if (roleName.equals("PARENT")) {
            return reclamationRepository.findByParent(currentUser);
        }

        if (roleName.equals("ADMIN")) {
            return reclamationRepository.findAll();
        }

        throw new RuntimeException("Accès refusé");
    }

    @Override
    public Reclamation getReclamationById(Long id) {
        User currentUser = getCurrentUser();

        Reclamation reclamation = reclamationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Réclamation introuvable"));

        String roleName = currentUser.getRole().getName().name();

        if (roleName.equals("ADMIN")) {
            return reclamation;
        }

        if (roleName.equals("PARENT")
                && reclamation.getParent() != null
                && reclamation.getParent().getId().equals(currentUser.getId())) {
            return reclamation;
        }

        throw new RuntimeException("Accès refusé à cette réclamation");
    }

    @Override
    public Reclamation updateReclamation(Long id, UpdateReclamationRequest request) {
        User currentUser = getCurrentUser();

        Reclamation reclamation = reclamationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Réclamation introuvable"));

        String roleName = currentUser.getRole().getName().name();

        boolean isOwner = reclamation.getParent() != null
                && reclamation.getParent().getId().equals(currentUser.getId());

        if (!roleName.equals("PARENT") || !isOwner) {
            throw new RuntimeException("Seul le parent propriétaire peut modifier cette réclamation");
        }

        if (reclamation.getStatus() == ReclamationStatus.RESOLVED
                || reclamation.getStatus() == ReclamationStatus.REJECTED) {
            throw new RuntimeException("Impossible de modifier une réclamation déjà clôturée");
        }

        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new RuntimeException("Le titre est obligatoire");
        }

        if (request.getDescription() == null || request.getDescription().trim().isEmpty()) {
            throw new RuntimeException("La description est obligatoire");
        }

        if (request.getCategory() == null || request.getCategory().trim().isEmpty()) {
            throw new RuntimeException("La catégorie est obligatoire");
        }

        reclamation.setTitle(request.getTitle().trim());
        reclamation.setDescription(request.getDescription().trim());

        try {
            reclamation.setCategory(
                    ReclamationCategory.valueOf(request.getCategory().trim().toUpperCase())
            );
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Catégorie invalide. Valeurs autorisées : REPAS, TRANSPORT, COMPORTEMENT, HYGIENE, SECURITE, PERSONNEL, AUTRE");
        }

        if (request.getPriority() != null && !request.getPriority().trim().isEmpty()) {
            try {
                reclamation.setPriority(
                        ReclamationPriority.valueOf(request.getPriority().trim().toUpperCase())
                );
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Priorité invalide. Valeurs autorisées : LOW, MEDIUM, HIGH");
            }
        }

        if (reclamation.getConversation() != null) {
            reclamation.getConversation().setSubject("Réclamation : " + request.getTitle().trim());
            conversationRepository.save(reclamation.getConversation());
        }

        return reclamationRepository.save(reclamation);
    }

    @Override
    public Reclamation updateStatus(Long reclamationId, UpdateReclamationStatusRequest request) {
        User currentUser = getCurrentUser();

        String roleName = currentUser.getRole().getName().name();

        if (!roleName.equals("ADMIN")) {
            throw new RuntimeException("Seul un admin peut modifier le statut d'une réclamation");
        }

        Reclamation reclamation = reclamationRepository.findById(reclamationId)
                .orElseThrow(() -> new RuntimeException("Réclamation introuvable"));

        if (request.getStatus() == null || request.getStatus().trim().isEmpty()) {
            throw new RuntimeException("Le statut est obligatoire");
        }

        try {
            reclamation.setStatus(
                    ReclamationStatus.valueOf(request.getStatus().trim().toUpperCase())
            );
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Statut invalide. Valeurs autorisées : OPEN, IN_PROGRESS, RESOLVED, REJECTED");
        }

        reclamation.setAssignedAdmin(currentUser);

        return reclamationRepository.save(reclamation);
    }

    @Override
    public void deleteReclamation(Long id) {
        User currentUser = getCurrentUser();

        Reclamation reclamation = reclamationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Réclamation introuvable"));

        String roleName = currentUser.getRole().getName().name();

        boolean isOwner = reclamation.getParent() != null
                && reclamation.getParent().getId().equals(currentUser.getId());

        if (!(roleName.equals("ADMIN") || (roleName.equals("PARENT") && isOwner))) {
            throw new RuntimeException("Accès refusé pour supprimer cette réclamation");
        }

        Conversation conversation = reclamation.getConversation();

        reclamationRepository.delete(reclamation);

        if (conversation != null) {
            conversationRepository.delete(conversation);
        }
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur connecté introuvable"));
    }
}