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
import com.tinyspring.garderie.dto.MlPredictionResponse;
import com.tinyspring.garderie.dto.UpdateReclamationRequest;
import com.tinyspring.garderie.dto.UpdateReclamationStatusRequest;
import com.tinyspring.garderie.entity.Conversation;
import com.tinyspring.garderie.entity.Reclamation;
import com.tinyspring.garderie.entity.ReclamationHistory;
import com.tinyspring.garderie.entity.User;
import com.tinyspring.garderie.entity.enums.ConversationStatus;
import com.tinyspring.garderie.entity.enums.ConversationType;
import com.tinyspring.garderie.entity.enums.DecisionRecommendation;
import com.tinyspring.garderie.entity.enums.ReclamationCategory;
import com.tinyspring.garderie.entity.enums.ReclamationHistoryActionType;
import com.tinyspring.garderie.entity.enums.ReclamationPriority;
import com.tinyspring.garderie.entity.enums.ReclamationStatus;
import com.tinyspring.garderie.repository.ConversationRepository;
import com.tinyspring.garderie.repository.ReclamationHistoryRepository;
import com.tinyspring.garderie.repository.ReclamationRepository;
import com.tinyspring.garderie.repository.UserRepository;
import com.tinyspring.garderie.service.BadWordFilterService;
import com.tinyspring.garderie.service.MlPredictionService;
import com.tinyspring.garderie.service.ReclamationService;
import org.apache.poi.ss.usermodel.Cell;
import com.tinyspring.garderie.dto.RecommendedAdminActionResponse;
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
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Service
public class ReclamationServiceImpl implements ReclamationService {

    private final ReclamationRepository reclamationRepository;
    private final ReclamationHistoryRepository reclamationHistoryRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final BadWordFilterService badWordFilterService;
    private final MlPredictionService mlPredictionService;

    public ReclamationServiceImpl(ReclamationRepository reclamationRepository,
                                  ReclamationHistoryRepository reclamationHistoryRepository,
                                  ConversationRepository conversationRepository,
                                  UserRepository userRepository,
                                  BadWordFilterService badWordFilterService,
                                  MlPredictionService mlPredictionService) {
        this.reclamationRepository = reclamationRepository;
        this.reclamationHistoryRepository = reclamationHistoryRepository;
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
        this.badWordFilterService = badWordFilterService;
        this.mlPredictionService = mlPredictionService;
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

        String cleanTitle = badWordFilterService.censorText(title.trim());
        String cleanDescription = badWordFilterService.censorText(description.trim());

        Conversation conversation = new Conversation();
        conversation.setSubject("Réclamation : " + cleanTitle);
        conversation.setType(ConversationType.RECLAMATION);
        conversation.setStatus(ConversationStatus.OPEN);
        conversation.setCreatedBy(currentUser);
        conversation.setParent(currentUser);

        Conversation savedConversation = conversationRepository.save(conversation);

        Reclamation reclamation = new Reclamation();
        reclamation.setTitle(cleanTitle);
        reclamation.setDescription(cleanDescription);
        reclamation.setParent(currentUser);
        reclamation.setConversation(savedConversation);
        reclamation.setStatus(ReclamationStatus.OPEN);
        reclamation.setAutoClassified(false);

        if (priority != null && !priority.trim().isEmpty()) {
            try {
                reclamation.setPriority(
                        ReclamationPriority.valueOf(priority.trim().toUpperCase())
                );
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Priorité invalide. Valeurs autorisées : LOW, MEDIUM, HIGH");
            }
        }

        if (category != null && !category.trim().isEmpty()) {
            try {
                reclamation.setCategory(
                        ReclamationCategory.valueOf(category.trim().toUpperCase())
                );
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Catégorie invalide");
            }
        }

        applyMlPrediction(reclamation, cleanTitle, cleanDescription, priority, category);

        detectRecurringReclamation(reclamation);

        handleImageUpload(reclamation, image);
        handleAttachmentUpload(reclamation, attachment);

        Reclamation savedReclamation = reclamationRepository.save(reclamation);

        addHistory(
                savedReclamation,
                ReclamationHistoryActionType.CREATED,
                "Réclamation créée",
                null,
                "Titre : " + savedReclamation.getTitle(),
                currentUser
        );

        if (savedReclamation.getPredictedCategory() != null) {
            addHistory(
                    savedReclamation,
                    ReclamationHistoryActionType.CATEGORY_CHANGED,
                    "Catégorie prédite par ML",
                    null,
                    savedReclamation.getPredictedCategory().name()
                            + " (confiance: "
                            + (savedReclamation.getClassificationConfidence() != null
                            ? savedReclamation.getClassificationConfidence()
                            : 0.0)
                            + ")",
                    null
            );
        }

        if (savedReclamation.getPredictedPriority() != null) {
            addHistory(
                    savedReclamation,
                    ReclamationHistoryActionType.PRIORITY_CHANGED,
                    "Priorité prédite par ML",
                    null,
                    savedReclamation.getPredictedPriority().name()
                            + " (confiance: "
                            + (savedReclamation.getPriorityConfidence() != null
                            ? savedReclamation.getPriorityConfidence()
                            : 0.0)
                            + ")",
                    null
            );
        }

        if (savedReclamation.getDecisionRecommendation() != null) {
            addHistory(
                    savedReclamation,
                    ReclamationHistoryActionType.DECISION_RECOMMENDED,
                    "Décision recommandée par ML",
                    null,
                    savedReclamation.getDecisionRecommendation().name()
                            + " (confiance: "
                            + (savedReclamation.getDecisionConfidence() != null
                            ? savedReclamation.getDecisionConfidence()
                            : 0.0)
                            + ")",
                    null
            );
        }

        if (Boolean.TRUE.equals(savedReclamation.getRecurring())) {
            addHistory(
                    savedReclamation,
                    ReclamationHistoryActionType.RECURRENCE_DETECTED,
                    "Réclamation récurrente détectée",
                    null,
                    savedReclamation.getRecurrenceReason(),
                    null
            );
        }

        return savedReclamation;
    }
    private void detectRecurringReclamation(Reclamation reclamation) {
        if (reclamation.getCategory() == null || reclamation.getCategory() == ReclamationCategory.AUTRE) {
            reclamation.setRecurring(false);
            reclamation.setRecurrenceCount(0);
            reclamation.setRecurrenceReason(null);
            return;
        }

        LocalDateTime since = LocalDateTime.now().minusDays(30);

        List<ReclamationStatus> activeStatuses = List.of(
                ReclamationStatus.OPEN,
                ReclamationStatus.IN_PROGRESS
        );

        List<Reclamation> possibleSimilarReclamations =
                reclamationRepository.findByCategoryAndCreatedAtAfterAndStatusIn(
                        reclamation.getCategory(),
                        since,
                        activeStatuses
                );

        String currentText = normalizeText(
                reclamation.getTitle() + " " + reclamation.getDescription()
        );

        int similarCount = 0;

        for (Reclamation existing : possibleSimilarReclamations) {
            String existingText = normalizeText(
                    existing.getTitle() + " " + existing.getDescription()
            );

            int commonWords = countCommonImportantWords(currentText, existingText);

            if (commonWords >= 2) {
                similarCount++;
            }
        }

        if (similarCount >= 2) {
            reclamation.setRecurring(true);
            reclamation.setRecurrenceCount(similarCount + 1);
            reclamation.setRecurrenceReason(
                    "Problème récurrent détecté : "
                            + (similarCount + 1)
                            + " réclamations similaires dans la catégorie "
                            + reclamation.getCategory().name()
                            + " durant les 30 derniers jours."
            );
        } else {
            reclamation.setRecurring(false);
            reclamation.setRecurrenceCount(similarCount);
            reclamation.setRecurrenceReason(null);
        }
    }

    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }

        return text.toLowerCase()
                .replaceAll("[^a-zA-Zàâçéèêëîïôûùüÿñæœ\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private int countCommonImportantWords(String text1, String text2) {
        Set<String> ignoredWords = new HashSet<>(Arrays.asList(
                "le", "la", "les", "un", "une", "des", "de", "du", "dans",
                "mon", "ma", "mes", "enfant", "enfants", "est", "sont",
                "avec", "pour", "sur", "par", "pas", "plus", "très",
                "reclamation", "problème", "probleme"
        ));

        Set<String> words1 = new HashSet<>(Arrays.asList(text1.split(" ")));
        Set<String> words2 = new HashSet<>(Arrays.asList(text2.split(" ")));

        words1.removeIf(word -> word.length() < 4 || ignoredWords.contains(word));
        words2.removeIf(word -> word.length() < 4 || ignoredWords.contains(word));

        words1.retainAll(words2);

        return words1.size();
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
            addInfoRow(infoTable, "Categorie predite", safeText(reclamation.getPredictedCategory() != null ? reclamation.getPredictedCategory().name() : null), smallBoldFont, bodyFont);
            addInfoRow(infoTable, "Confiance categorie", reclamation.getClassificationConfidence() != null ? String.valueOf(reclamation.getClassificationConfidence()) : "-", smallBoldFont, bodyFont);
            addInfoRow(infoTable, "Priorite", safeText(reclamation.getPriority() != null ? reclamation.getPriority().name() : null), smallBoldFont, bodyFont);
            addInfoRow(infoTable, "Priorite predite", safeText(reclamation.getPredictedPriority() != null ? reclamation.getPredictedPriority().name() : null), smallBoldFont, bodyFont);
            addInfoRow(infoTable, "Confiance priorite", reclamation.getPriorityConfidence() != null ? String.valueOf(reclamation.getPriorityConfidence()) : "-", smallBoldFont, bodyFont);
            addInfoRow(infoTable, "Decision recommandee", safeText(reclamation.getDecisionRecommendation() != null ? reclamation.getDecisionRecommendation().name() : null), smallBoldFont, bodyFont);
            addInfoRow(infoTable, "Confiance decision", reclamation.getDecisionConfidence() != null ? String.valueOf(reclamation.getDecisionConfidence()) : "-", smallBoldFont, bodyFont);
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
    public String generateSuggestedAdminResponse(Long reclamationId) {
        User currentUser = getCurrentUser();
        String roleName = currentUser.getRole().getName().name();

        if (!roleName.equals("ADMIN")) {
            throw new RuntimeException("Seul un admin peut générer une réponse suggérée");
        }

        Reclamation reclamation = reclamationRepository.findById(reclamationId)
                .orElseThrow(() -> new RuntimeException("Réclamation introuvable"));

        String greeting = "Bonjour,\n\n";
        String intro = buildIntroByStatus(reclamation);
        String categoryPart = buildCategorySpecificMessage(reclamation);
        String priorityPart = buildPrioritySpecificMessage(reclamation);
        String decisionPart = buildDecisionSpecificMessage(reclamation);
        String sensitiveKeywordsPart = buildSensitiveKeywordsMessage(reclamation);
        String closing = "\nNous restons à votre disposition pour toute information complémentaire.\n\nCordialement,\nL'administration.";

        return greeting + intro + categoryPart + priorityPart + decisionPart + sensitiveKeywordsPart + closing;
    }
    @Override
    public RecommendedAdminActionResponse getRecommendedAdminAction(Long reclamationId) {
        User currentUser = getCurrentUser();
        String roleName = currentUser.getRole().getName().name();

        if (!roleName.equals("ADMIN")) {
            throw new RuntimeException("Seul un admin peut consulter l'action recommandée");
        }

        Reclamation reclamation = reclamationRepository.findById(reclamationId)
                .orElseThrow(() -> new RuntimeException("Réclamation introuvable"));

        if (reclamation.getDecisionRecommendation() == null) {
            return new RecommendedAdminActionResponse(
                    "ANALYSE_ADMINISTRATIVE",
                    "MEDIUM",
                    "Analyser manuellement la réclamation et déterminer le service concerné.",
                    "24H"
            );
        }

        return switch (reclamation.getDecisionRecommendation()) {
            case MEDICAL_ATTENTION -> new RecommendedAdminActionResponse(
                    "SERVICE_MEDICAL",
                    "HIGH",
                    "Vérifier immédiatement l'état de l'enfant et informer les responsables concernés.",
                    "IMMEDIATE"
            );

            case REPAIR_NEEDED -> new RecommendedAdminActionResponse(
                    "MAINTENANCE",
                    "HIGH",
                    "Sécuriser la zone ou l'équipement concerné et planifier une réparation rapide.",
                    "24H"
            );

            case INCREASE_SUPERVISION -> new RecommendedAdminActionResponse(
                    "SERVICE_PEDAGOGIQUE",
                    "HIGH",
                    "Renforcer immédiatement la surveillance dans la zone ou le contexte signalé.",
                    "IMMEDIATE"
            );

            case STAFF_TRAINING -> new RecommendedAdminActionResponse(
                    "RESSOURCES_HUMAINES",
                    "MEDIUM",
                    "Identifier le personnel concerné et prévoir une action de sensibilisation ou de formation.",
                    "72H"
            );

            case PROCESS_IMPROVEMENT -> new RecommendedAdminActionResponse(
                    "ADMINISTRATION",
                    "MEDIUM",
                    "Analyser le processus interne concerné et proposer une amélioration organisationnelle.",
                    "72H"
            );

            case ADMINISTRATIVE_CORRECTION -> new RecommendedAdminActionResponse(
                    "SERVICE_ADMINISTRATIF",
                    "MEDIUM",
                    "Vérifier le dossier et corriger les informations ou traitements administratifs concernés.",
                    "24H"
            );

            case TRANSPORT_ESCALATION -> new RecommendedAdminActionResponse(
                    "SERVICE_TRANSPORT",
                    "HIGH",
                    "Contacter le responsable transport et vérifier l'incident signalé en priorité.",
                    "IMMEDIATE"
            );

            case PARENT_FOLLOWUP -> new RecommendedAdminActionResponse(
                    "RELATION_PARENT",
                    "LOW",
                    "Contacter le parent pour obtenir des précisions complémentaires avant décision finale.",
                    "48H"
            );
        };
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
                    "Categorie predite",
                    "Confiance categorie",
                    "Priorite",
                    "Priorite predite",
                    "Confiance priorite",
                    "Decision recommandee",
                    "Confiance decision",
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
                        reclamation.getPredictedCategory() != null ? reclamation.getPredictedCategory().name() : "-"
                );
                row.createCell(5).setCellValue(
                        reclamation.getClassificationConfidence() != null ? reclamation.getClassificationConfidence() : 0.0
                );
                row.createCell(6).setCellValue(
                        reclamation.getPriority() != null ? reclamation.getPriority().name() : "-"
                );
                row.createCell(7).setCellValue(
                        reclamation.getPredictedPriority() != null ? reclamation.getPredictedPriority().name() : "-"
                );
                row.createCell(8).setCellValue(
                        reclamation.getPriorityConfidence() != null ? reclamation.getPriorityConfidence() : 0.0
                );
                row.createCell(9).setCellValue(
                        reclamation.getDecisionRecommendation() != null ? reclamation.getDecisionRecommendation().name() : "-"
                );
                row.createCell(10).setCellValue(
                        reclamation.getDecisionConfidence() != null ? reclamation.getDecisionConfidence() : 0.0
                );
                row.createCell(11).setCellValue(
                        reclamation.getStatus() != null ? reclamation.getStatus().name() : "-"
                );
                row.createCell(12).setCellValue(
                        reclamation.getParent() != null ? safeExcelText(reclamation.getParent().getEmail()) : "-"
                );
                row.createCell(13).setCellValue(
                        reclamation.getAssignedAdmin() != null ? safeExcelText(reclamation.getAssignedAdmin().getEmail()) : "-"
                );

                Cell adminCommentCell = row.createCell(14);
                adminCommentCell.setCellValue(safeExcelText(reclamation.getAdminComment()));
                adminCommentCell.setCellStyle(wrapStyle);

                row.createCell(15).setCellValue(
                        reclamation.getImageName() != null ? safeExcelText(reclamation.getImageName()) : "-"
                );
                row.createCell(16).setCellValue(
                        reclamation.getAttachmentName() != null ? safeExcelText(reclamation.getAttachmentName()) : "-"
                );
                row.createCell(17).setCellValue(
                        reclamation.getCreatedAt() != null ? reclamation.getCreatedAt().toString() : "-"
                );
                row.createCell(18).setCellValue(
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

            String cleanTitle = badWordFilterService.censorText(request.getTitle().trim());
            String cleanDescription = badWordFilterService.censorText(request.getDescription().trim());

            String oldTitle = reclamation.getTitle();
            String oldDescription = reclamation.getDescription();
            String oldCategory = reclamation.getCategory() != null ? reclamation.getCategory().name() : null;
            String oldPriority = reclamation.getPriority() != null ? reclamation.getPriority().name() : null;
            String oldPredictedCategory = reclamation.getPredictedCategory() != null ? reclamation.getPredictedCategory().name() : null;
            String oldPredictedPriority = reclamation.getPredictedPriority() != null ? reclamation.getPredictedPriority().name() : null;
            Double oldConfidence = reclamation.getClassificationConfidence();
            Double oldPriorityConfidence = reclamation.getPriorityConfidence();
            String oldDecisionRecommendation = reclamation.getDecisionRecommendation() != null
                    ? reclamation.getDecisionRecommendation().name()
                    : null;
            Double oldDecisionConfidence = reclamation.getDecisionConfidence();

            reclamation.setTitle(cleanTitle);
            reclamation.setDescription(cleanDescription);

            reclamation.setCategory(null);
            if (request.getCategory() != null && !request.getCategory().trim().isEmpty()) {
                try {
                    reclamation.setCategory(
                            ReclamationCategory.valueOf(request.getCategory().trim().toUpperCase())
                    );
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("Catégorie invalide");
                }
            }

            reclamation.setPriority(null);
            if (request.getPriority() != null && !request.getPriority().trim().isEmpty()) {
                try {
                    reclamation.setPriority(
                            ReclamationPriority.valueOf(request.getPriority().trim().toUpperCase())
                    );
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("Priorité invalide. Valeurs autorisées : LOW, MEDIUM, HIGH");
                }
            }

            applyMlPrediction(
                    reclamation,
                    cleanTitle,
                    cleanDescription,
                    request.getPriority(),
                    request.getCategory()
            );

            if (reclamation.getConversation() != null) {
                reclamation.getConversation().setSubject("Réclamation : " + cleanTitle);
                conversationRepository.save(reclamation.getConversation());
            }

            Reclamation saved = reclamationRepository.save(reclamation);

            if (!safeEquals(oldTitle, saved.getTitle())) {
                addHistory(saved, ReclamationHistoryActionType.TITLE_CHANGED, "Titre modifié", oldTitle, saved.getTitle(), currentUser);
            }

            if (!safeEquals(oldDescription, saved.getDescription())) {
                addHistory(saved, ReclamationHistoryActionType.DESCRIPTION_UPDATED, "Description mise à jour", oldDescription, saved.getDescription(), currentUser);
            }

            String newCategory = saved.getCategory() != null ? saved.getCategory().name() : null;
            if (!safeEquals(oldCategory, newCategory)) {
                addHistory(saved, ReclamationHistoryActionType.CATEGORY_CHANGED, "Catégorie changée", oldCategory, newCategory, currentUser);
            }

            String newPriority = saved.getPriority() != null ? saved.getPriority().name() : null;
            if (!safeEquals(oldPriority, newPriority)) {
                addHistory(saved, ReclamationHistoryActionType.PRIORITY_CHANGED, "Priorité changée", oldPriority, newPriority, currentUser);
            }

            String newPredictedCategory = saved.getPredictedCategory() != null ? saved.getPredictedCategory().name() : null;
            boolean confidenceChanged = (oldConfidence == null && saved.getClassificationConfidence() != null)
                    || (oldConfidence != null && saved.getClassificationConfidence() != null
                    && Double.compare(oldConfidence, saved.getClassificationConfidence()) != 0);

            if (!safeEquals(oldPredictedCategory, newPredictedCategory) || confidenceChanged) {
                addHistory(
                        saved,
                        ReclamationHistoryActionType.CATEGORY_CHANGED,
                        "Prédiction catégorie ML mise à jour",
                        oldPredictedCategory != null ? oldPredictedCategory : "-",
                        newPredictedCategory != null
                                ? newPredictedCategory + " (confiance: " + saved.getClassificationConfidence() + ")"
                                : "-",
                        null
                );
            }

            String newPredictedPriority = saved.getPredictedPriority() != null ? saved.getPredictedPriority().name() : null;
            boolean priorityConfidenceChanged = (oldPriorityConfidence == null && saved.getPriorityConfidence() != null)
                    || (oldPriorityConfidence != null && saved.getPriorityConfidence() != null
                    && Double.compare(oldPriorityConfidence, saved.getPriorityConfidence()) != 0);

            if (!safeEquals(oldPredictedPriority, newPredictedPriority) || priorityConfidenceChanged) {
                addHistory(
                        saved,
                        ReclamationHistoryActionType.PRIORITY_CHANGED,
                        "Prédiction priorité ML mise à jour",
                        oldPredictedPriority != null ? oldPredictedPriority : "-",
                        newPredictedPriority != null
                                ? newPredictedPriority + " (confiance: " + saved.getPriorityConfidence() + ")"
                                : "-",
                        null
                );
            }

            String newDecisionRecommendation = saved.getDecisionRecommendation() != null
                    ? saved.getDecisionRecommendation().name()
                    : null;

            boolean decisionConfidenceChanged = (oldDecisionConfidence == null && saved.getDecisionConfidence() != null)
                    || (oldDecisionConfidence != null && saved.getDecisionConfidence() != null
                    && Double.compare(oldDecisionConfidence, saved.getDecisionConfidence()) != 0);

            if (!safeEquals(oldDecisionRecommendation, newDecisionRecommendation) || decisionConfidenceChanged) {
                addHistory(
                        saved,
                        ReclamationHistoryActionType.DECISION_RECOMMENDED,
                        "Décision recommandée ML mise à jour",
                        oldDecisionRecommendation != null ? oldDecisionRecommendation : "-",
                        newDecisionRecommendation != null
                                ? newDecisionRecommendation + " (confiance: " + saved.getDecisionConfidence() + ")"
                                : "-",
                        null
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
                addHistory(saved, ReclamationHistoryActionType.ADMIN_COMMENT_ADDED, "Réponse administrative ajoutée", null, saved.getAdminComment(), currentUser);
            } else if (oldAdminComment != null && !oldAdminComment.isBlank()
                    && saved.getAdminComment() != null && !saved.getAdminComment().isBlank()
                    && !safeEquals(oldAdminComment, saved.getAdminComment())) {
                addHistory(saved, ReclamationHistoryActionType.ADMIN_COMMENT_UPDATED, "Réponse administrative modifiée", oldAdminComment, saved.getAdminComment(), currentUser);
            } else if (oldAdminComment != null && !oldAdminComment.isBlank()
                    && (saved.getAdminComment() == null || saved.getAdminComment().isBlank())) {
                addHistory(saved, ReclamationHistoryActionType.ADMIN_COMMENT_REMOVED, "Réponse administrative supprimée", oldAdminComment, null, currentUser);
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
            addHistory(saved, ReclamationHistoryActionType.STATUS_CHANGED, "Statut changé", oldStatus, newStatus, currentUser);
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

    private void applyMlPrediction(Reclamation reclamation,
                                   String cleanTitle,
                                   String cleanDescription,
                                   String requestedPriority,
                                   String requestedCategory) {

        MlPredictionResponse categoryResponse = mlPredictionService.predictCategory(cleanTitle, cleanDescription);
        MlPredictionResponse priorityResponse = mlPredictionService.predictPriority(cleanTitle, cleanDescription);

        if (categoryResponse == null && priorityResponse == null) {
            reclamation.setAutoClassified(false);

            if (reclamation.getCategory() == null) {
                reclamation.setCategory(ReclamationCategory.AUTRE);
            }

            if (reclamation.getPriority() == null) {
                reclamation.setPriority(ReclamationPriority.MEDIUM);
            }

            reclamation.setDecisionRecommendation(null);
            reclamation.setDecisionConfidence(null);
            return;
        }

        boolean categoryAppliedByMl = false;

        if (categoryResponse != null && categoryResponse.getPredictedCategory() != null) {
            try {
                ReclamationCategory predictedCat = ReclamationCategory.valueOf(
                        categoryResponse.getPredictedCategory().trim().toUpperCase()
                );

                reclamation.setPredictedCategory(predictedCat);
                reclamation.setClassificationConfidence(categoryResponse.getClassificationConfidence());

                if (requestedCategory == null || requestedCategory.trim().isEmpty()) {
                    reclamation.setCategory(predictedCat);
                    categoryAppliedByMl = true;
                }
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (priorityResponse != null && priorityResponse.getPredictedPriority() != null) {
            try {
                ReclamationPriority predictedPrio = ReclamationPriority.valueOf(
                        priorityResponse.getPredictedPriority().trim().toUpperCase()
                );

                reclamation.setPredictedPriority(predictedPrio);
                reclamation.setPriorityConfidence(priorityResponse.getPriorityConfidence());

                if (requestedPriority == null || requestedPriority.trim().isEmpty()) {
                    reclamation.setPriority(predictedPrio);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (reclamation.getCategory() == null) {
            reclamation.setCategory(ReclamationCategory.AUTRE);
        }

        if (reclamation.getPriority() == null) {
            reclamation.setPriority(ReclamationPriority.MEDIUM);
        }

        reclamation.setAutoClassified(categoryAppliedByMl);

        try {
            MlPredictionResponse decisionResponse = mlPredictionService.predictDecision(
                    cleanTitle,
                    cleanDescription,
                    reclamation.getCategory().name(),
                    reclamation.getPriority().name()
            );

            if (decisionResponse != null && decisionResponse.getDecisionRecommendation() != null) {
                try {
                    DecisionRecommendation decisionRecommendation = DecisionRecommendation.valueOf(
                            decisionResponse.getDecisionRecommendation().trim().toUpperCase()
                    );

                    reclamation.setDecisionRecommendation(decisionRecommendation);
                    reclamation.setDecisionConfidence(decisionResponse.getDecisionConfidence());
                } catch (IllegalArgumentException ignored) {
                    reclamation.setDecisionRecommendation(null);
                    reclamation.setDecisionConfidence(null);
                }
            } else {
                reclamation.setDecisionRecommendation(null);
                reclamation.setDecisionConfidence(null);
            }

        } catch (Exception e) {
            reclamation.setDecisionRecommendation(null);
            reclamation.setDecisionConfidence(null);
        }
    }

    private void handleImageUpload(Reclamation reclamation, MultipartFile image) {
        boolean hasImage = image != null && !image.isEmpty();
        if (!hasImage) {
            return;
        }

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

    private void handleAttachmentUpload(Reclamation reclamation, MultipartFile attachment) {
        boolean hasAttachment = attachment != null && !attachment.isEmpty();
        if (!hasAttachment) {
            return;
        }

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

    private String buildIntroByStatus(Reclamation reclamation) {
        if (reclamation.getStatus() == null) {
            return "Nous avons bien reçu votre réclamation.\n\n";
        }

        return switch (reclamation.getStatus()) {
            case OPEN -> "Nous avons bien reçu votre réclamation et elle a été prise en compte.\n\n";
            case IN_PROGRESS -> "Votre réclamation est actuellement en cours de traitement par le service concerné.\n\n";
            case RESOLVED -> "Après vérification, le problème signalé a été traité.\n\n";
            case REJECTED -> "Après analyse de votre demande, nous vous informons qu'elle n'a pas pu être retenue en l'état.\n\n";
        };
    }

    private String buildCategorySpecificMessage(Reclamation reclamation) {
        if (reclamation.getCategory() == null) {
            return "Une vérification complémentaire est en cours afin de vous apporter une réponse adaptée.\n\n";
        }

        return switch (reclamation.getCategory()) {
            case TRANSPORT -> "Une vérification a été engagée auprès du service transport afin de comprendre la situation signalée.\n\n";
            case REPAS -> "Le service de restauration a été informé afin de vérifier l'incident signalé et prendre les mesures nécessaires.\n\n";
            case HYGIENE -> "Le signalement a été transmis à l'équipe concernée afin de contrôler les conditions d'hygiène mentionnées.\n\n";
            case SECURITE -> "La situation signalée fait l'objet d'une attention particulière compte tenu de son impact potentiel sur la sécurité des enfants.\n\n";
            case FINANCIER -> "Une vérification administrative et financière est en cours concernant les éléments mentionnés dans votre réclamation.\n\n";
            case ADMINISTRATIF -> "Votre dossier administratif est en cours de vérification afin d'identifier l'origine du problème signalé.\n\n";
            case PEDAGOGIQUE -> "Le contenu de votre réclamation a été transmis au service pédagogique pour analyse et suivi.\n\n";
            case PERSONNEL -> "Le signalement a été transmis à la direction afin qu'un suivi approprié soit effectué.\n\n";
            case COMPORTEMENT -> "Une analyse de la situation a été engagée afin d'évaluer les faits signalés et de prendre les mesures adaptées.\n\n";
            case AUTRE -> "Votre demande est en cours d'analyse par l'administration afin de vous apporter une réponse adaptée.\n\n";
        };
    }

    private String buildPrioritySpecificMessage(Reclamation reclamation) {
        if (reclamation.getPriority() == null) {
            return "";
        }

        return switch (reclamation.getPriority()) {
            case HIGH -> "Compte tenu du niveau de priorité de cette réclamation, un traitement rapide est en cours.\n\n";
            case MEDIUM -> "Cette réclamation est en cours de traitement avec le niveau d'attention approprié.\n\n";
            case LOW -> "Cette demande sera traitée dans les meilleurs délais par le service concerné.\n\n";
        };
    }

    private String buildDecisionSpecificMessage(Reclamation reclamation) {
        if (reclamation.getDecisionRecommendation() == null) {
            return "";
        }

        return switch (reclamation.getDecisionRecommendation()) {
            case REPAIR_NEEDED ->
                    "Une intervention technique ou matérielle a été identifiée comme nécessaire afin de corriger durablement le problème signalé.\n\n";
            case INCREASE_SUPERVISION ->
                    "Un renforcement temporaire ou ciblé de la surveillance sera mis en place afin de sécuriser davantage les enfants dans la situation signalée.\n\n";
            case STAFF_TRAINING ->
                    "Une action de sensibilisation ou de renforcement des compétences du personnel concerné sera envisagée afin d'améliorer la qualité de prise en charge.\n\n";
            case PROCESS_IMPROVEMENT ->
                    "Une amélioration du processus interne a été identifiée afin d'éviter que ce type de situation ne se reproduise.\n\n";
            case ADMINISTRATIVE_CORRECTION ->
                    "Une correction administrative sera effectuée sur les éléments du dossier ou du traitement concernés.\n\n";
            case MEDICAL_ATTENTION ->
                    "Une attention particulière sera portée à l'aspect médical ou sanitaire de la situation afin de garantir la sécurité et le bien-être de l'enfant.\n\n";
            case TRANSPORT_ESCALATION ->
                    "Le signalement sera transmis au service transport pour traitement prioritaire et vérification approfondie.\n\n";
            case PARENT_FOLLOWUP ->
                    "Un complément d'information pourra être demandé afin de mieux qualifier la situation et assurer un suivi adapté.\n\n";
        };
    }

    private String buildSensitiveKeywordsMessage(Reclamation reclamation) {
        if (reclamation.getDescription() == null || reclamation.getDescription().isBlank()) {
            return "";
        }

        String text = reclamation.getDescription().toLowerCase();

        if (text.contains("allerg")) {
            return "Compte tenu des éléments liés à une possible allergie, une vérification renforcée sera effectuée avec la plus grande vigilance.\n\n";
        }

        if (text.contains("danger") || text.contains("dangereux")) {
            return "Les éléments signalant un danger potentiel feront l'objet d'une vérification prioritaire afin de prévenir tout risque.\n\n";
        }

        if (text.contains("retard")) {
            return "Les retards mentionnés seront vérifiés avec le service concerné afin d'améliorer la régularité et l'organisation.\n\n";
        }

        if (text.contains("bless") || text.contains("malade") || text.contains("malaise")) {
            return "Les éléments relatifs à la santé ou à un incident physique seront examinés avec une attention immédiate.\n\n";
        }

        return "";
    }
}