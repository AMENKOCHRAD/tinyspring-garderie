package com.tinyspring.garderie.service.impl;

import com.tinyspring.garderie.dto.EscalationResult;
import com.tinyspring.garderie.dto.AdminDashboardResponse;
import com.tinyspring.garderie.dto.MlPredictionResponse;
import com.tinyspring.garderie.dto.RecommendedAdminActionResponse;
import com.tinyspring.garderie.dto.SmartPriorityResult;
import com.tinyspring.garderie.dto.UpdateReclamationRequest;
import com.tinyspring.garderie.dto.UpdateReclamationStatusRequest;
import com.tinyspring.garderie.entity.Conversation;
import com.tinyspring.garderie.entity.Reclamation;
import com.tinyspring.garderie.entity.ReclamationHistory;
import com.tinyspring.garderie.entity.Role;
import com.tinyspring.garderie.entity.RoleName;
import com.tinyspring.garderie.entity.User;
import com.tinyspring.garderie.entity.enums.DecisionRecommendation;
import com.tinyspring.garderie.entity.enums.ReclamationCategory;
import com.tinyspring.garderie.entity.enums.ReclamationHistoryActionType;
import com.tinyspring.garderie.entity.enums.ReclamationPriority;
import com.tinyspring.garderie.entity.enums.ReclamationStatus;
import com.tinyspring.garderie.entity.enums.SmartPriorityLevel;
import com.tinyspring.garderie.repository.ConversationRepository;
import com.tinyspring.garderie.repository.ReclamationHistoryRepository;
import com.tinyspring.garderie.repository.ReclamationRepository;
import com.tinyspring.garderie.repository.UserRepository;
import com.tinyspring.garderie.service.BadWordFilterService;
import com.tinyspring.garderie.service.EscalationService;
import com.tinyspring.garderie.service.MlPredictionService;
import com.tinyspring.garderie.service.SmartPriorityEngine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReclamationServiceImplTest {

    private final ReclamationRepository reclamationRepository = mock(ReclamationRepository.class);
    private final ReclamationHistoryRepository historyRepository = mock(ReclamationHistoryRepository.class);
    private final ConversationRepository conversationRepository = mock(ConversationRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final BadWordFilterService badWordFilterService = mock(BadWordFilterService.class);
    private final MlPredictionService mlPredictionService = mock(MlPredictionService.class);
    private final SmartPriorityEngine smartPriorityEngine = mock(SmartPriorityEngine.class);
    private final EscalationService escalationService = mock(EscalationService.class);

    private final ReclamationServiceImpl service = new ReclamationServiceImpl(
            reclamationRepository,
            historyRepository,
            conversationRepository,
            userRepository,
            badWordFilterService,
            mlPredictionService,
            smartPriorityEngine,
            escalationService
    );

    private User admin;
    private User parent;

    @BeforeEach
    void setUp() {
        admin = user(1L, "admin@garderie.com", RoleName.ADMIN);
        parent = user(2L, "parent@garderie.com", RoleName.PARENT);

        when(userRepository.findByEmail("admin@garderie.com")).thenReturn(Optional.of(admin));
        when(userRepository.findByEmail("parent@garderie.com")).thenReturn(Optional.of(parent));
        when(reclamationRepository.save(any(Reclamation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(escalationService.evaluate(any(Reclamation.class))).thenReturn(
                EscalationResult.builder()
                        .escalationRequired(false)
                        .recommendedService("ADMINISTRATION")
                        .recommendedUrgency("LOW")
                        .recommendedDelay("NONE")
                        .recommendedAction("Surveillance")
                        .escalationReason("Aucune regle critique")
                        .build()
        );
        when(escalationService.evaluateAndApply(any(Reclamation.class))).thenReturn(
                EscalationResult.builder().escalationRequired(false).build()
        );
        when(smartPriorityEngine.calculateAndApply(any(Reclamation.class))).thenAnswer(invocation -> {
            Reclamation reclamation = invocation.getArgument(0);
            reclamation.setSmartPriorityScore(42);
            reclamation.setSmartPriorityLevel(SmartPriorityLevel.MEDIUM);
            reclamation.setSmartPriorityReason("Score 42/100 [MEDIUM]");
            return SmartPriorityResult.builder()
                    .totalScore(42)
                    .level(SmartPriorityLevel.MEDIUM)
                    .reason("Score 42/100 [MEDIUM]")
                    .build();
        });
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createReclamationAppliesMlUploadsRecurrenceAndEscalation() {
        authenticate("parent@garderie.com");
        when(badWordFilterService.censorText(any(String.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mlPredictionService.predictCategory(any(String.class), any(String.class)))
                .thenReturn(categoryPrediction("SECURITE", 0.91));
        when(mlPredictionService.predictPriority(any(String.class), any(String.class)))
                .thenReturn(priorityPrediction("HIGH", 0.88));
        when(mlPredictionService.predictDecision(any(String.class), any(String.class), any(String.class), any(String.class)))
                .thenReturn(decisionPrediction("INCREASE_SUPERVISION", 0.79));
        when(reclamationRepository.findByCategoryAndCreatedAtAfterAndStatusIn(
                any(ReclamationCategory.class),
                any(LocalDateTime.class),
                anyList()
        )).thenReturn(List.of(
                existingSimilar("Cour glissante escalier", "La cour glissante pres de escalier", ReclamationCategory.SECURITE),
                existingSimilar("Escalier cour dangereuse", "Probleme escalier cour", ReclamationCategory.SECURITE)
        ));
        when(escalationService.evaluateAndApply(any(Reclamation.class))).thenAnswer(invocation -> {
            Reclamation reclamation = invocation.getArgument(0);
            reclamation.setAutoEscalated(true);
            reclamation.setEscalationReason("Danger securite");
            reclamation.setRecommendedService("DIRECTION");
            reclamation.setRecommendedDelay("IMMEDIATE");
            reclamation.setRecommendedAction("Verifier la zone");
            return EscalationResult.builder()
                    .escalationRequired(true)
                    .escalationReason("Danger securite")
                    .recommendedService("DIRECTION")
                    .recommendedUrgency("HIGH")
                    .recommendedDelay("IMMEDIATE")
                    .recommendedAction("Verifier la zone")
                    .build();
        });

        MockMultipartFile image = new MockMultipartFile("image", "photo cour.png", "image/png", "img".getBytes());
        MockMultipartFile attachment = new MockMultipartFile("attachment", "rapport parent.pdf", "application/pdf", "pdf".getBytes());

        Reclamation saved = service.createReclamation(
                " Cour glissante escalier ",
                "La cour glissante pres de escalier est dangereuse",
                null,
                null,
                image,
                attachment
        );

        assertEquals("Cour glissante escalier", saved.getTitle());
        assertEquals(ReclamationCategory.SECURITE, saved.getCategory());
        assertEquals(ReclamationPriority.HIGH, saved.getPriority());
        assertEquals(DecisionRecommendation.INCREASE_SUPERVISION, saved.getDecisionRecommendation());
        assertTrue(saved.getAutoClassified());
        assertTrue(saved.getRecurring());
        assertEquals(3, saved.getRecurrenceCount());
        assertTrue(saved.getImagePath().startsWith("/uploads/reclamations/"));
        assertTrue(saved.getAttachmentPath().startsWith("/uploads/reclamations/attachments/"));
        assertTrue(saved.getAutoEscalated());
        verify(historyRepository, atLeastOnce()).save(any(ReclamationHistory.class));
    }

    @Test
    void createReclamationRejectsInvalidInputsAndNonParent() {
        authenticate("admin@garderie.com");
        assertThrows(RuntimeException.class,
                () -> service.createReclamation("Titre", "Description", null, null, null, null));

        authenticate("parent@garderie.com");
        assertThrows(RuntimeException.class,
                () -> service.createReclamation(" ", "Description", null, null, null, null));
        assertThrows(RuntimeException.class,
                () -> service.createReclamation("Titre", " ", null, null, null, null));
        assertThrows(RuntimeException.class,
                () -> service.createReclamation("Titre", "Description", "URGENT", null, null, null));
        assertThrows(RuntimeException.class,
                () -> service.createReclamation("Titre", "Description", null, "BAD", null, null));
    }

    @Test
    void getMyReclamationsReturnsParentOrAdminDataDependingOnRole() {
        Reclamation parentReclamation = sampleReclamation();
        when(reclamationRepository.findByParent(parent)).thenReturn(List.of(parentReclamation));

        authenticate("parent@garderie.com");
        assertEquals(1, service.getMyReclamations().size());

        authenticate("admin@garderie.com");
        when(reclamationRepository.findAll()).thenReturn(List.of(parentReclamation, new Reclamation()));
        assertEquals(2, service.getMyReclamations().size());
    }

    @Test
    void getReclamationHistoryChecksAccessAndReturnsTimeline() {
        Reclamation reclamation = sampleReclamation();
        ReclamationHistory history = ReclamationHistory.builder()
                .actionType(ReclamationHistoryActionType.STATUS_CHANGED)
                .actionLabel("Statut change")
                .createdAt(LocalDateTime.now())
                .build();
        when(reclamationRepository.findById(10L)).thenReturn(Optional.of(reclamation));
        when(historyRepository.findByReclamationOrderByCreatedAtDesc(reclamation)).thenReturn(List.of(history));

        authenticate("admin@garderie.com");

        assertSame(reclamation, service.getReclamationById(10L));
        assertEquals(1, service.getReclamationHistory(10L).size());
    }

    @Test
    void generateSuggestedAdminResponseCombinesStatusCategoryPriorityDecisionAndKeywords() {
        authenticate("admin@garderie.com");
        Reclamation reclamation = sampleReclamation();
        reclamation.setStatus(ReclamationStatus.IN_PROGRESS);
        reclamation.setCategory(ReclamationCategory.TRANSPORT);
        reclamation.setPriority(ReclamationPriority.HIGH);
        reclamation.setDecisionRecommendation(DecisionRecommendation.TRANSPORT_ESCALATION);
        reclamation.setDescription("Le bus a un retard dangereux");
        when(reclamationRepository.findById(10L)).thenReturn(Optional.of(reclamation));

        String response = service.generateSuggestedAdminResponse(10L);

        assertTrue(response.contains("Bonjour"));
        assertTrue(response.toLowerCase().contains("transport"));
        assertTrue(response.contains("Cordialement"));
    }

    @Test
    void getRecommendedAdminActionCoversDefaultAndAllDecisionRoutes() {
        authenticate("admin@garderie.com");
        Reclamation reclamation = sampleReclamation();
        when(reclamationRepository.findById(10L)).thenReturn(Optional.of(reclamation));

        RecommendedAdminActionResponse defaultAction = service.getRecommendedAdminAction(10L);
        assertEquals("ANALYSE_ADMINISTRATIVE", defaultAction.getRecommendedService());

        for (DecisionRecommendation decision : DecisionRecommendation.values()) {
            reclamation.setDecisionRecommendation(decision);
            RecommendedAdminActionResponse action = service.getRecommendedAdminAction(10L);
            assertNotNull(action.getRecommendedService());
            assertNotNull(action.getRecommendedAction());
        }
    }

    @Test
    void getEscalationInfoPrefersPersistedValuesAndFallbackEvaluationValues() {
        authenticate("admin@garderie.com");
        Reclamation reclamation = sampleReclamation();
        reclamation.setAutoEscalated(true);
        reclamation.setEscalationReason("Persisted reason");
        reclamation.setRecommendedService("DIRECTION");
        reclamation.setRecommendedDelay("24H");
        reclamation.setRecommendedAction("Action persisted");
        reclamation.setSmartPriorityScore(90);
        reclamation.setSmartPriorityLevel(SmartPriorityLevel.CRITICAL);
        reclamation.setSmartPriorityReason("Critical reason");
        reclamation.setRecurring(true);
        reclamation.setRecurrenceCount(4);
        when(reclamationRepository.findById(10L)).thenReturn(Optional.of(reclamation));

        var info = service.getEscalationInfo(10L);

        assertTrue(info.getAutoEscalated());
        assertEquals("Persisted reason", info.getEscalationReason());
        assertEquals("DIRECTION", info.getRecommendedService());
        assertEquals("CRITICAL", info.getSmartPriorityLevel());
        assertTrue(info.getRecurring());
    }

    @Test
    void exportReclamationHistoryPdfAndExcelReturnFiles() {
        authenticate("admin@garderie.com");
        Reclamation reclamation = sampleReclamation();
        reclamation.setCreatedAt(LocalDateTime.now());
        reclamation.setUpdatedAt(LocalDateTime.now());

        ReclamationHistory history = ReclamationHistory.builder()
                .actionLabel("Creation")
                .actorName("Admin")
                .actorRole("ADMIN")
                .oldValue(null)
                .newValue("OPEN")
                .createdAt(LocalDateTime.now())
                .build();

        when(reclamationRepository.findById(10L)).thenReturn(Optional.of(reclamation));
        when(historyRepository.findByReclamationOrderByCreatedAtDesc(reclamation)).thenReturn(List.of(history));
        when(reclamationRepository.findAll()).thenReturn(List.of(reclamation));

        assertTrue(service.exportReclamationHistoryPdf(10L).length > 100);
        assertTrue(service.exportReclamationsExcel().length > 100);
    }

    @Test
    void parentUpdateReclamationReappliesMlAndCreatesDetailedHistory() {
        authenticate("parent@garderie.com");
        Reclamation reclamation = sampleReclamation();
        reclamation.setTitle("Ancien titre");
        reclamation.setDescription("Ancienne description");
        reclamation.setCategory(ReclamationCategory.ADMINISTRATIF);
        reclamation.setPriority(ReclamationPriority.LOW);
        reclamation.setPredictedCategory(ReclamationCategory.ADMINISTRATIF);
        reclamation.setClassificationConfidence(0.2);
        reclamation.setPredictedPriority(ReclamationPriority.LOW);
        reclamation.setPriorityConfidence(0.3);
        reclamation.setDecisionRecommendation(DecisionRecommendation.ADMINISTRATIVE_CORRECTION);
        reclamation.setDecisionConfidence(0.4);

        when(reclamationRepository.findById(10L)).thenReturn(Optional.of(reclamation));
        when(badWordFilterService.censorText(any(String.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mlPredictionService.predictCategory(any(String.class), any(String.class)))
                .thenReturn(categoryPrediction("REPAS", 0.93));
        when(mlPredictionService.predictPriority(any(String.class), any(String.class)))
                .thenReturn(priorityPrediction("HIGH", 0.87));
        when(mlPredictionService.predictDecision(any(String.class), any(String.class), any(String.class), any(String.class)))
                .thenReturn(decisionPrediction("PROCESS_IMPROVEMENT", 0.74));

        Reclamation updated = service.updateReclamation(
                10L,
                UpdateReclamationRequest.builder()
                        .title(" Nouveau titre repas ")
                        .description("Nouvelle description repas")
                        .category("")
                        .priority("")
                        .build()
        );

        assertEquals("Nouveau titre repas", updated.getTitle());
        assertEquals(ReclamationCategory.REPAS, updated.getCategory());
        assertEquals(ReclamationPriority.HIGH, updated.getPriority());
        assertEquals(DecisionRecommendation.PROCESS_IMPROVEMENT, updated.getDecisionRecommendation());
        verify(conversationRepository).save(reclamation.getConversation());
        verify(historyRepository, atLeastOnce()).save(any(ReclamationHistory.class));
    }

    @Test
    void parentUpdateRejectsClosedReclamationAndInvalidFields() {
        authenticate("parent@garderie.com");
        Reclamation reclamation = sampleReclamation();
        when(reclamationRepository.findById(10L)).thenReturn(Optional.of(reclamation));

        reclamation.setStatus(ReclamationStatus.RESOLVED);
        assertThrows(RuntimeException.class,
                () -> service.updateReclamation(10L, UpdateReclamationRequest.builder()
                        .title("Titre").description("Description").build()));

        reclamation.setStatus(ReclamationStatus.OPEN);
        assertThrows(RuntimeException.class,
                () -> service.updateReclamation(10L, UpdateReclamationRequest.builder()
                        .title(" ").description("Description").build()));
        assertThrows(RuntimeException.class,
                () -> service.updateReclamation(10L, UpdateReclamationRequest.builder()
                        .title("Titre").description(" ").build()));
        assertThrows(RuntimeException.class,
                () -> service.updateReclamation(10L, UpdateReclamationRequest.builder()
                        .title("Titre").description("Description").category("BAD").build()));
        assertThrows(RuntimeException.class,
                () -> service.updateReclamation(10L, UpdateReclamationRequest.builder()
                        .title("Titre").description("Description").priority("BAD").build()));
    }

    @Test
    void updateStatusChangesStatusAssignsAdminAndCreatesHistory() {
        authenticate("admin@garderie.com");
        Reclamation reclamation = sampleReclamation();
        reclamation.setStatus(ReclamationStatus.OPEN);
        when(reclamationRepository.findById(10L)).thenReturn(Optional.of(reclamation));

        Reclamation saved = service.updateStatus(10L, new UpdateReclamationStatusRequest("RESOLVED"));

        assertEquals(ReclamationStatus.RESOLVED, saved.getStatus());
        assertSame(admin, saved.getAssignedAdmin());
        verify(historyRepository, atLeastOnce()).save(any(ReclamationHistory.class));
    }

    @Test
    void updateReclamationAsAdminAddsAndRemovesAdminComment() {
        authenticate("admin@garderie.com");
        Reclamation reclamation = sampleReclamation();
        when(reclamationRepository.findById(10L)).thenReturn(Optional.of(reclamation));

        Reclamation withComment = service.updateReclamation(
                10L,
                UpdateReclamationRequest.builder().adminComment("Traitement effectue").build()
        );

        assertEquals("Traitement effectue", withComment.getAdminComment());
        assertSame(admin, withComment.getAssignedAdmin());

        Reclamation withoutComment = service.updateReclamation(
                10L,
                UpdateReclamationRequest.builder().adminComment(" ").build()
        );

        assertEquals(null, withoutComment.getAdminComment());
    }

    @Test
    void deleteReclamationDeletesHistoryAndLinkedConversation() {
        authenticate("admin@garderie.com");
        Reclamation reclamation = sampleReclamation();
        when(reclamationRepository.findById(10L)).thenReturn(Optional.of(reclamation));
        when(historyRepository.findByReclamationOrderByCreatedAtDesc(reclamation))
                .thenReturn(List.of(new ReclamationHistory()));

        service.deleteReclamation(10L);

        verify(historyRepository).deleteAll(anyList());
        verify(reclamationRepository).delete(reclamation);
        verify(conversationRepository).delete(reclamation.getConversation());
    }

    @Test
    void adminDashboardAggregatesRepositoryMetrics() {
        authenticate("admin@garderie.com");
        Reclamation security = sampleReclamation();
        security.setCategory(ReclamationCategory.SECURITE);
        Reclamation meal = sampleReclamation();
        meal.setCategory(ReclamationCategory.REPAS);

        when(reclamationRepository.findActiveReclamationsSortedBySmartPriority(anyList()))
                .thenReturn(List.of(security, meal));
        when(reclamationRepository.countByStatusInAndSmartPriorityLevel(anyList(), any(SmartPriorityLevel.class)))
                .thenReturn(1L);
        when(reclamationRepository.countActiveWithScoreGreaterOrEqual(anyList(), anyInt())).thenReturn(2L);
        when(reclamationRepository.countRecurringActive(anyList())).thenReturn(3L);
        when(reclamationRepository.countUnassignedActive(anyList())).thenReturn(4L);
        when(reclamationRepository.countByStatus(ReclamationStatus.OPEN)).thenReturn(5L);
        when(reclamationRepository.countByStatus(ReclamationStatus.IN_PROGRESS)).thenReturn(6L);
        when(reclamationRepository.countByStatus(ReclamationStatus.RESOLVED)).thenReturn(7L);
        when(reclamationRepository.countByStatus(ReclamationStatus.REJECTED)).thenReturn(8L);
        when(reclamationRepository.averageSmartPriorityScore(anyList())).thenReturn(42.345);
        when(reclamationRepository.maxSmartPriorityScore(anyList())).thenReturn(99);

        AdminDashboardResponse dashboard = service.getAdminDashboard();

        assertEquals(2, dashboard.getTotalActive());
        assertEquals(5, dashboard.getOpenCount());
        assertEquals(42.35, dashboard.getAverageSmartScore());
        assertEquals(99, dashboard.getMaxSmartScore());
        assertEquals(1L, dashboard.getActiveByCategory().get("SECURITE"));
        assertEquals(1L, dashboard.getActiveByCategory().get("REPAS"));

        authenticate("parent@garderie.com");
        assertThrows(RuntimeException.class, service::getAdminDashboard);
    }

    private Reclamation sampleReclamation() {
        Reclamation reclamation = new Reclamation();
        ReflectionTestUtils.setField(reclamation, "id", 10L);
        reclamation.setTitle("Retard transport");
        reclamation.setDescription("Description");
        reclamation.setParent(parent);
        reclamation.setCategory(ReclamationCategory.ADMINISTRATIF);
        reclamation.setPriority(ReclamationPriority.MEDIUM);
        reclamation.setStatus(ReclamationStatus.OPEN);
        reclamation.setConversation(new com.tinyspring.garderie.entity.Conversation());
        return reclamation;
    }

    private Reclamation existingSimilar(String title, String description, ReclamationCategory category) {
        Reclamation reclamation = new Reclamation();
        reclamation.setTitle(title);
        reclamation.setDescription(description);
        reclamation.setCategory(category);
        reclamation.setStatus(ReclamationStatus.OPEN);
        return reclamation;
    }

    private MlPredictionResponse categoryPrediction(String category, double confidence) {
        MlPredictionResponse response = new MlPredictionResponse();
        response.setPredictedCategory(category);
        response.setClassificationConfidence(confidence);
        return response;
    }

    private MlPredictionResponse priorityPrediction(String priority, double confidence) {
        MlPredictionResponse response = new MlPredictionResponse();
        response.setPredictedPriority(priority);
        response.setPriorityConfidence(confidence);
        return response;
    }

    private MlPredictionResponse decisionPrediction(String decision, double confidence) {
        MlPredictionResponse response = new MlPredictionResponse();
        response.setDecisionRecommendation(decision);
        response.setDecisionConfidence(confidence);
        return response;
    }

    private User user(Long id, String email, RoleName roleName) {
        User user = new User("User", email, "secret", true, new Role(roleName));
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private void authenticate(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, "password")
        );
    }
}
