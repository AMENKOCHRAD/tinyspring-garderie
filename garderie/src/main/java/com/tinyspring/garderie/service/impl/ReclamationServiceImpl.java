package com.tinyspring.garderie.service.impl;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.tinyspring.garderie.dto.UpdateReclamationRequest;
import com.tinyspring.garderie.dto.UpdateReclamationStatusRequest;
import com.tinyspring.garderie.entity.Conversation;
import com.tinyspring.garderie.entity.Reclamation;
import com.tinyspring.garderie.entity.ReclamationHistory;
import com.tinyspring.garderie.entity.User;
import com.tinyspring.garderie.entity.enums.ConversationStatus;
import com.tinyspring.garderie.entity.enums.ConversationType;
import com.tinyspring.garderie.entity.enums.ReclamationCategory;
import com.tinyspring.garderie.entity.enums.ReclamationHistoryActionType;
import com.tinyspring.garderie.entity.enums.ReclamationPriority;
import com.tinyspring.garderie.entity.enums.ReclamationStatus;
import com.tinyspring.garderie.repository.ConversationRepository;
import com.tinyspring.garderie.repository.ReclamationHistoryRepository;
import com.tinyspring.garderie.repository.ReclamationRepository;
import com.tinyspring.garderie.repository.UserRepository;
import com.tinyspring.garderie.service.ReclamationService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class ReclamationServiceImpl implements ReclamationService {

    private final ReclamationRepository reclamationRepository;
    private final ReclamationHistoryRepository reclamationHistoryRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;

    public ReclamationServiceImpl(ReclamationRepository reclamationRepository,
                                  ReclamationHistoryRepository reclamationHistoryRepository,
                                  ConversationRepository conversationRepository,
                                  UserRepository userRepository) {
        this.reclamationRepository = reclamationRepository;
        this.reclamationHistoryRepository = reclamationHistoryRepository;
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
                throw new RuntimeException("Erreur upload pièce jointe réclamation : " + e.getMessage(), e);
            }
        }

        Reclamation savedReclamation = reclamationRepository.save(reclamation);

        addHistory(
                savedReclamation,
                ReclamationHistoryActionType.CREATED,
                "Réclamation créée",
                null,
                "Titre : " + savedReclamation.getTitle(),
                currentUser
        );

        return savedReclamation;
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

        checkAccessToReclamation(currentUser, reclamation);
        return reclamation;
    }

    @Override
    public List<ReclamationHistory> getReclamationHistory(Long reclamationId) {
        User currentUser = getCurrentUser();

        Reclamation reclamation = reclamationRepository.findById(reclamationId)
                .orElseThrow(() -> new RuntimeException("Réclamation introuvable"));

        checkAccessToReclamation(currentUser, reclamation);

        return reclamationHistoryRepository.findByReclamationOrderByCreatedAtDesc(reclamation);
    }

    @Override
    public byte[] exportReclamationHistoryPdf(Long reclamationId) {
        User currentUser = getCurrentUser();

        Reclamation reclamation = reclamationRepository.findById(reclamationId)
                .orElseThrow(() -> new RuntimeException("Réclamation introuvable"));

        checkAccessToReclamation(currentUser, reclamation);

        List<ReclamationHistory> histories =
                reclamationHistoryRepository.findByReclamationOrderByCreatedAtDesc(reclamation);

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            Document document = new Document(PageSize.A4, 36, 36, 50, 40);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
            Font subTitleFont = new Font(Font.HELVETICA, 11, Font.NORMAL, Color.DARK_GRAY);
            Font sectionFont = new Font(Font.HELVETICA, 13, Font.BOLD);
            Font bodyFont = new Font(Font.HELVETICA, 10, Font.NORMAL);
            Font smallBoldFont = new Font(Font.HELVETICA, 10, Font.BOLD);

            Paragraph title = new Paragraph("Historique de reclamation", titleFont);
            title.setSpacingAfter(6f);
            document.add(title);

            Paragraph subtitle = new Paragraph(
                    "Reclamation #" + reclamation.getId() + " - " + reclamation.getTitle(),
                    subTitleFont
            );
            subtitle.setSpacingAfter(16f);
            document.add(subtitle);

            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingAfter(18f);
            infoTable.setWidths(new float[]{1.5f, 3.5f});

            addInfoRow(infoTable, "Titre", safeText(reclamation.getTitle()), smallBoldFont, bodyFont);
            addInfoRow(infoTable, "Categorie", safeText(reclamation.getCategory() != null ? reclamation.getCategory().name() : null), smallBoldFont, bodyFont);
            addInfoRow(infoTable, "Priorite", safeText(reclamation.getPriority() != null ? reclamation.getPriority().name() : null), smallBoldFont, bodyFont);
            addInfoRow(infoTable, "Statut", safeText(reclamation.getStatus() != null ? reclamation.getStatus().name() : null), smallBoldFont, bodyFont);
            addInfoRow(infoTable, "Parent", reclamation.getParent() != null ? safeText(reclamation.getParent().getEmail()) : "-", smallBoldFont, bodyFont);

            document.add(infoTable);

            Paragraph section = new Paragraph("Timeline des evenements", sectionFont);
            section.setSpacingAfter(10f);
            document.add(section);

            if (histories.isEmpty()) {
                Paragraph empty = new Paragraph("Aucun historique disponible.", bodyFont);
                document.add(empty);
            } else {
                PdfPTable historyTable = new PdfPTable(5);
                historyTable.setWidthPercentage(100);
                historyTable.setWidths(new float[]{1.6f, 2.2f, 1.7f, 2.2f, 2.3f});

                addHeaderCell(historyTable, "Date");
                addHeaderCell(historyTable, "Action");
                addHeaderCell(historyTable, "Acteur");
                addHeaderCell(historyTable, "Ancienne valeur");
                addHeaderCell(historyTable, "Nouvelle valeur");

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

                for (ReclamationHistory history : histories) {
                    addBodyCell(historyTable, history.getCreatedAt() != null ? history.getCreatedAt().format(formatter) : "-");
                    addBodyCell(historyTable, safeText(history.getActionLabel()));
                    addBodyCell(historyTable, safeText(history.getActorName()) + " (" + safeText(history.getActorRole()) + ")");
                    addBodyCell(historyTable, safeText(history.getOldValue()));
                    addBodyCell(historyTable, safeText(history.getNewValue()));
                }

                document.add(historyTable);
            }

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la generation du PDF de l'historique : " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] exportReclamationsExcel() {
        User currentUser = getCurrentUser();
        String roleName = currentUser.getRole().getName().name();

        if (!roleName.equals("ADMIN")) {
            throw new RuntimeException("Seul un admin peut exporter la liste des réclamations");
        }

        List<Reclamation> reclamations = reclamationRepository.findAll();

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Reclamations");

            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle wrapStyle = workbook.createCellStyle();
            wrapStyle.setWrapText(true);
            wrapStyle.setVerticalAlignment(VerticalAlignment.TOP);

            Row headerRow = sheet.createRow(0);
            String[] headers = {
                    "ID",
                    "Titre",
                    "Description",
                    "Categorie",
                    "Priorite",
                    "Statut",
                    "Parent",
                    "Admin assigne",
                    "Commentaire admin",
                    "Image",
                    "Piece jointe",
                    "Date creation",
                    "Date mise a jour"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            for (Reclamation reclamation : reclamations) {
                Row row = sheet.createRow(rowIndex++);

                row.createCell(0).setCellValue(reclamation.getId() != null ? reclamation.getId() : 0);
                row.createCell(1).setCellValue(safeExcelText(reclamation.getTitle()));

                Cell descriptionCell = row.createCell(2);
                descriptionCell.setCellValue(safeExcelText(reclamation.getDescription()));
                descriptionCell.setCellStyle(wrapStyle);

                row.createCell(3).setCellValue(
                        reclamation.getCategory() != null ? reclamation.getCategory().name() : "-"
                );
                row.createCell(4).setCellValue(
                        reclamation.getPriority() != null ? reclamation.getPriority().name() : "-"
                );
                row.createCell(5).setCellValue(
                        reclamation.getStatus() != null ? reclamation.getStatus().name() : "-"
                );
                row.createCell(6).setCellValue(
                        reclamation.getParent() != null ? safeExcelText(reclamation.getParent().getEmail()) : "-"
                );
                row.createCell(7).setCellValue(
                        reclamation.getAssignedAdmin() != null ? safeExcelText(reclamation.getAssignedAdmin().getEmail()) : "-"
                );

                Cell adminCommentCell = row.createCell(8);
                adminCommentCell.setCellValue(safeExcelText(reclamation.getAdminComment()));
                adminCommentCell.setCellStyle(wrapStyle);

                row.createCell(9).setCellValue(
                        reclamation.getImageName() != null ? safeExcelText(reclamation.getImageName()) : "-"
                );
                row.createCell(10).setCellValue(
                        reclamation.getAttachmentName() != null ? safeExcelText(reclamation.getAttachmentName()) : "-"
                );
                row.createCell(11).setCellValue(
                        reclamation.getCreatedAt() != null ? reclamation.getCreatedAt().toString() : "-"
                );
                row.createCell(12).setCellValue(
                        reclamation.getUpdatedAt() != null ? reclamation.getUpdatedAt().toString() : "-"
                );
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                int currentWidth = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, Math.min(currentWidth + 1000, 20000));
            }

            workbook.write(baos);
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la generation du fichier Excel : " + e.getMessage(), e);
        }
    }

    @Override
    public Reclamation updateReclamation(Long id, UpdateReclamationRequest request) {
        User currentUser = getCurrentUser();

        Reclamation reclamation = reclamationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Réclamation introuvable"));

        String roleName = currentUser.getRole().getName().name();

        boolean isOwner = reclamation.getParent() != null
                && reclamation.getParent().getId().equals(currentUser.getId());

        if (roleName.equals("PARENT")) {
            if (!isOwner) {
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

            String oldTitle = reclamation.getTitle();
            String oldDescription = reclamation.getDescription();
            String oldCategory = reclamation.getCategory() != null ? reclamation.getCategory().name() : null;
            String oldPriority = reclamation.getPriority() != null ? reclamation.getPriority().name() : null;

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

            Reclamation saved = reclamationRepository.save(reclamation);

            if (!safeEquals(oldTitle, saved.getTitle())) {
                addHistory(
                        saved,
                        ReclamationHistoryActionType.TITLE_CHANGED,
                        "Titre modifié",
                        oldTitle,
                        saved.getTitle(),
                        currentUser
                );
            }

            if (!safeEquals(oldDescription, saved.getDescription())) {
                addHistory(
                        saved,
                        ReclamationHistoryActionType.DESCRIPTION_UPDATED,
                        "Description mise à jour",
                        oldDescription,
                        saved.getDescription(),
                        currentUser
                );
            }

            String newCategory = saved.getCategory() != null ? saved.getCategory().name() : null;
            if (!safeEquals(oldCategory, newCategory)) {
                addHistory(
                        saved,
                        ReclamationHistoryActionType.CATEGORY_CHANGED,
                        "Catégorie changée",
                        oldCategory,
                        newCategory,
                        currentUser
                );
            }

            String newPriority = saved.getPriority() != null ? saved.getPriority().name() : null;
            if (!safeEquals(oldPriority, newPriority)) {
                addHistory(
                        saved,
                        ReclamationHistoryActionType.PRIORITY_CHANGED,
                        "Priorité changée",
                        oldPriority,
                        newPriority,
                        currentUser
                );
            }

            return saved;
        }

        if (roleName.equals("ADMIN")) {
            String oldAdminComment = reclamation.getAdminComment();
            String newAdminComment = request.getAdminComment();

            reclamation.setAdminComment(
                    newAdminComment != null && !newAdminComment.trim().isEmpty()
                            ? newAdminComment.trim()
                            : null
            );

            reclamation.setAssignedAdmin(currentUser);

            Reclamation saved = reclamationRepository.save(reclamation);

            if ((oldAdminComment == null || oldAdminComment.isBlank())
                    && saved.getAdminComment() != null && !saved.getAdminComment().isBlank()) {
                addHistory(
                        saved,
                        ReclamationHistoryActionType.ADMIN_COMMENT_ADDED,
                        "Réponse administrative ajoutée",
                        null,
                        saved.getAdminComment(),
                        currentUser
                );
            } else if (oldAdminComment != null && !oldAdminComment.isBlank()
                    && saved.getAdminComment() != null && !saved.getAdminComment().isBlank()
                    && !safeEquals(oldAdminComment, saved.getAdminComment())) {
                addHistory(
                        saved,
                        ReclamationHistoryActionType.ADMIN_COMMENT_UPDATED,
                        "Réponse administrative modifiée",
                        oldAdminComment,
                        saved.getAdminComment(),
                        currentUser
                );
            } else if (oldAdminComment != null && !oldAdminComment.isBlank()
                    && (saved.getAdminComment() == null || saved.getAdminComment().isBlank())) {
                addHistory(
                        saved,
                        ReclamationHistoryActionType.ADMIN_COMMENT_REMOVED,
                        "Réponse administrative supprimée",
                        oldAdminComment,
                        null,
                        currentUser
                );
            }

            return saved;
        }

        throw new RuntimeException("Accès refusé");
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

        String oldStatus = reclamation.getStatus() != null ? reclamation.getStatus().name() : null;

        try {
            reclamation.setStatus(
                    ReclamationStatus.valueOf(request.getStatus().trim().toUpperCase())
            );
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Statut invalide. Valeurs autorisées : OPEN, IN_PROGRESS, RESOLVED, REJECTED");
        }

        reclamation.setAssignedAdmin(currentUser);

        Reclamation saved = reclamationRepository.save(reclamation);

        String newStatus = saved.getStatus() != null ? saved.getStatus().name() : null;

        if (!safeEquals(oldStatus, newStatus)) {
            addHistory(
                    saved,
                    ReclamationHistoryActionType.STATUS_CHANGED,
                    "Statut changé",
                    oldStatus,
                    newStatus,
                    currentUser
            );
        }

        return saved;
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

        List<ReclamationHistory> histories = reclamationHistoryRepository.findByReclamationOrderByCreatedAtDesc(reclamation);
        if (!histories.isEmpty()) {
            reclamationHistoryRepository.deleteAll(histories);
        }

        reclamationRepository.delete(reclamation);

        if (conversation != null) {
            conversationRepository.delete(conversation);
        }
    }

    private void addHistory(Reclamation reclamation,
                            ReclamationHistoryActionType actionType,
                            String actionLabel,
                            String oldValue,
                            String newValue,
                            User actor) {
        ReclamationHistory history = ReclamationHistory.builder()
                .reclamation(reclamation)
                .actionType(actionType)
                .actionLabel(actionLabel)
                .oldValue(oldValue)
                .newValue(newValue)
                .actorName(actor != null ? actor.getEmail() : "Système")
                .actorRole(actor != null ? actor.getRole().getName().name() : "SYSTEM")
                .build();

        reclamationHistoryRepository.save(history);
    }

    private void checkAccessToReclamation(User currentUser, Reclamation reclamation) {
        String roleName = currentUser.getRole().getName().name();

        if (roleName.equals("ADMIN")) {
            return;
        }

        if (roleName.equals("PARENT")
                && reclamation.getParent() != null
                && reclamation.getParent().getId().equals(currentUser.getId())) {
            return;
        }

        throw new RuntimeException("Accès refusé à cette réclamation");
    }

    private boolean safeEquals(String a, String b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.equals(b);
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur connecté introuvable"));
    }

    private void addInfoRow(PdfPTable table,
                            String label,
                            String value,
                            Font labelFont,
                            Font valueFont) {
        PdfPCell cell1 = new PdfPCell(new Phrase(label, labelFont));
        PdfPCell cell2 = new PdfPCell(new Phrase(value, valueFont));

        cell1.setPadding(8f);
        cell2.setPadding(8f);

        table.addCell(cell1);
        table.addCell(cell2);
    }

    private void addHeaderCell(PdfPTable table, String text) {
        Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
        PdfPCell cell = new PdfPCell(new Phrase(text, headerFont));
        cell.setBackgroundColor(new Color(37, 99, 235));
        cell.setPadding(8f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    private void addBodyCell(PdfPTable table, String text) {
        Font bodyFont = new Font(Font.HELVETICA, 9, Font.NORMAL);
        PdfPCell cell = new PdfPCell(new Phrase(text, bodyFont));
        cell.setPadding(8f);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    private String safeText(String value) {
        return (value == null || value.trim().isEmpty()) ? "-" : value;
    }

    private String safeExcelText(String value) {
        return (value == null || value.trim().isEmpty()) ? "-" : value;
    }
}