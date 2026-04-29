package com.tinyspring.garderie.controller.boutique;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.tinyspring.garderie.dto.boutique.InteractionRequestDto;
import com.tinyspring.garderie.dto.boutique.ProduitDto;
import com.tinyspring.garderie.entity.User;
import com.tinyspring.garderie.entity.boutique.Categorie;
import com.tinyspring.garderie.entity.boutique.Produit;
import com.tinyspring.garderie.repository.UserRepository;
import com.tinyspring.garderie.repository.boutique.CommandeProduitRepository;
import com.tinyspring.garderie.repository.boutique.ProduitRepository;
import com.tinyspring.garderie.service.boutique.AffiniteService;
import com.tinyspring.garderie.service.boutique.CommandeService;
import com.tinyspring.garderie.service.boutique.OllamaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests controllers boutique additionnels")
class AdditionalBoutiqueControllerTest {

    @Mock
    private OllamaService ollamaService;

    @Mock
    private ProduitRepository produitRepository;

    @Mock
    private CommandeProduitRepository commandeProduitRepository;

    @Mock
    private AffiniteService affiniteService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CommandeService commandeService;

    private MarketingAdvisorController marketingAdvisorController;
    private FileController fileController;
    private RecommandationController recommandationController;
    private StripeWebhookController stripeWebhookController;
    private Path uploadDir;

    @BeforeEach
    void setUp() {
        marketingAdvisorController = new MarketingAdvisorController(
                ollamaService,
                produitRepository,
                commandeProduitRepository,
                new ObjectMapper()
        );
        fileController = new FileController();
        uploadDir = Path.of(
                "target",
                "test-uploads",
                "additional-controller-test",
                UUID.randomUUID().toString()
        );
        ReflectionTestUtils.setField(fileController, "uploadDir", uploadDir.toString());

        recommandationController = new RecommandationController(
                affiniteService,
                userRepository
        );
        stripeWebhookController = new StripeWebhookController(commandeService);
        ReflectionTestUtils.setField(stripeWebhookController, "webhookSecret", "whsec_test");
    }

    @AfterEach
    void tearDown() throws IOException {
        if (uploadDir == null || Files.notExists(uploadDir)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(uploadDir)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                            // Best effort cleanup for generated test files.
                        }
                    });
        }
    }

    @Test
    @DisplayName("analyser() retourne les suggestions triees")
    void analyser_shouldReturnSortedSuggestions() {
        Produit puzzle = produit(1L, "Puzzle bois", 12.50, 3);
        Produit livre = produit(2L, "Livre images", 8.90, 5);
        Produit ballon = produit(3L, "Ballon", 5.0, 20);
        when(produitRepository.findAll()).thenReturn(List.of(puzzle, livre, ballon));
        when(commandeProduitRepository.countVentesDepuis(eq(1L), any(LocalDateTime.class)))
                .thenReturn(0);
        when(commandeProduitRepository.countVentesDepuis(eq(2L), any(LocalDateTime.class)))
                .thenReturn(1);
        when(commandeProduitRepository.countVentesDepuis(eq(3L), any(LocalDateTime.class)))
                .thenReturn(4);
        when(ollamaService.analyserProduit(
                eq("Puzzle bois"),
                any(),
                eq(12.50),
                eq(3),
                eq(0),
                eq("Jeux educatifs")))
                .thenReturn("{\"diagnostic\":\"A\",\"score_urgence\":4}");
        when(ollamaService.analyserProduit(
                eq("Livre images"),
                any(),
                eq(8.90),
                eq(5),
                eq(1),
                eq("Jeux educatifs")))
                .thenReturn("{\"diagnostic\":\"B\",\"score_urgence\":9}");

        ResponseEntity<List<Map>> response = marketingAdvisorController.analyser();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody().get(0))
                .containsEntry("produit_id", 2L)
                .containsEntry("produit_nom", "Livre images")
                .containsEntry("ventes_recentes", 1);
        assertThat(response.getBody().get(1))
                .containsEntry("produit_id", 1L)
                .containsEntry("stock_actuel", 3);
    }

    @Test
    @DisplayName("analyser() ignore une suggestion illisible")
    void analyser_shouldIgnoreInvalidSuggestion() {
        Produit puzzle = produit(1L, "Puzzle bois", 12.50, 3);
        when(produitRepository.findAll()).thenReturn(List.of(puzzle));
        when(commandeProduitRepository.countVentesDepuis(eq(1L), any(LocalDateTime.class)))
                .thenReturn(0);
        when(ollamaService.analyserProduit(
                eq("Puzzle bois"),
                any(),
                eq(12.50),
                eq(3),
                eq(0),
                eq("Jeux educatifs")))
                .thenReturn("json invalide");

        ResponseEntity<List<Map>> response = marketingAdvisorController.analyser();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    @DisplayName("automatiser() applique description, promotion et tags")
    void automatiser_shouldApplyRequestedActions() {
        Produit produit = produit(1L, "Puzzle bois", 100.0, 3);
        when(produitRepository.findById(1L)).thenReturn(Optional.of(produit));
        Map<String, Object> suggestion = Map.of(
                "nouvelle_description", "Nouvelle description",
                "promotion_recommandee", 20,
                "tags_suggeres", List.of("garderie", "bois", "motricite")
        );
        Map<String, Object> body = Map.of(
                "types", List.of("DESCRIPTION", "PROMOTION", "TAGS"),
                "suggestion", suggestion
        );

        ResponseEntity<Map> response =
                marketingAdvisorController.automatiser(1L, body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("produit_nom", "Puzzle bois");
        assertThat((List<?>) response.getBody().get("actions_effectuees")).hasSize(3);
        assertThat(produit.getDescription()).isEqualTo("Nouvelle description");
        assertThat(produit.getPrix()).isEqualTo(80.0);
        assertThat(produit.getTags()).isEqualTo("garderie,bois,motricite");
        verify(produitRepository).save(produit);
    }

    @Test
    @DisplayName("analyserUnProduit() retourne la suggestion enrichie")
    void analyserUnProduit_shouldReturnSuggestion() {
        Produit produit = produit(1L, "Puzzle bois", 12.50, 3);
        when(produitRepository.findById(1L)).thenReturn(Optional.of(produit));
        when(ollamaService.analyserProduit(
                eq("Puzzle bois"),
                any(),
                eq(12.50),
                eq(3),
                eq(0),
                eq("Jeux educatifs")))
                .thenReturn("{\"diagnostic\":\"A\",\"score_urgence\":4}");

        ResponseEntity<Map> response =
                marketingAdvisorController.analyserUnProduit(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .containsEntry("produit_id", 1L)
                .containsEntry("produit_nom", "Puzzle bois")
                .containsEntry("score_urgence", 4);
    }

    @Test
    @DisplayName("analyserUnProduit() retourne une erreur lisible")
    void analyserUnProduit_shouldReturnErrorWhenOllamaFails() {
        Produit produit = produit(1L, "Puzzle bois", 12.50, 3);
        when(produitRepository.findById(1L)).thenReturn(Optional.of(produit));
        when(ollamaService.analyserProduit(
                eq("Puzzle bois"),
                any(),
                eq(12.50),
                eq(3),
                eq(0),
                eq("Jeux educatifs")))
                .thenThrow(new RuntimeException("ollama down"));

        ResponseEntity<Map> response =
                marketingAdvisorController.analyserUnProduit(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("error", "ollama down");
    }

    @Test
    @DisplayName("serveFile() retourne une image png")
    void serveFile_shouldReturnPngResource() throws IOException {
        Files.createDirectories(uploadDir);
        Files.writeString(uploadDir.resolve("image.png"), "png", StandardCharsets.UTF_8);

        ResponseEntity<Resource> response = fileController.serveFile("image.png");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_PNG);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("serveFile() retourne une image webp")
    void serveFile_shouldReturnWebpResource() throws IOException {
        Files.createDirectories(uploadDir);
        Files.writeString(uploadDir.resolve("image.webp"), "webp", StandardCharsets.UTF_8);

        ResponseEntity<Resource> response = fileController.serveFile("image.webp");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType().toString()).isEqualTo("image/webp");
    }

    @Test
    @DisplayName("serveFile() retourne 404 quand le fichier manque")
    void serveFile_shouldReturn404WhenMissing() {
        ResponseEntity<Resource> response = fileController.serveFile("missing.jpg");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("getRecommandes() retourne les produits recommandes")
    void getRecommandes_shouldReturnRecommendations() {
        User user = user(7L);
        UserDetails userDetails = userDetails("parent@example.com");
        ProduitDto produitDto = ProduitDto.builder()
                .id(1L)
                .nom("Puzzle bois")
                .build();
        when(userRepository.findByEmail("parent@example.com"))
                .thenReturn(Optional.of(user));
        when(affiniteService.getProduitRecommandes(7L))
                .thenReturn(List.of(produitDto));

        ResponseEntity<List<ProduitDto>> response =
                recommandationController.getRecommandes(userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(produitDto);
    }

    @Test
    @DisplayName("interaction() enregistre l'interaction utilisateur")
    void interaction_shouldRegisterInteraction() {
        User user = user(7L);
        UserDetails userDetails = userDetails("parent@example.com");
        InteractionRequestDto dto = new InteractionRequestDto();
        dto.setProduitId(1L);
        dto.setTypeInteraction("CLIC_DETAIL");
        when(userRepository.findByEmail("parent@example.com"))
                .thenReturn(Optional.of(user));

        ResponseEntity<Void> response =
                recommandationController.interaction(userDetails, dto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(affiniteService).enregistrerInteraction(7L, dto);
    }

    @Test
    @DisplayName("handleWebhook() retourne 400 sur signature invalide")
    void handleWebhook_shouldReturn400WhenSignatureInvalid() throws Exception {
        String payload = "{}";
        try (MockedStatic<Webhook> webhook = mockStatic(Webhook.class)) {
            webhook.when(() -> Webhook.constructEvent(payload, "bad", "whsec_test"))
                    .thenThrow(new SignatureVerificationException("bad signature", "bad"));

            ResponseEntity<String> response =
                    stripeWebhookController.handleWebhook(payload, "bad");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isEqualTo("Signature invalide");
        }
    }

    @Test
    @DisplayName("handleWebhook() marque la commande payee")
    void handleWebhook_shouldMarkCommandeAsPaid() throws Exception {
        String payload = """
                {"data":{"object":{
                  "id":"sess_123",
                  "client_reference_id":"42",
                  "payment_intent":"pi_123"
                }}}
                """;
        Event event = new Event();
        event.setType("checkout.session.completed");

        try (MockedStatic<Webhook> webhook = mockStatic(Webhook.class)) {
            webhook.when(() -> Webhook.constructEvent(payload, "sig", "whsec_test"))
                    .thenReturn(event);

            ResponseEntity<String> response =
                    stripeWebhookController.handleWebhook(payload, "sig");

            ArgumentCaptor<Session> sessionCaptor = ArgumentCaptor.forClass(Session.class);
            verify(commandeService).markAsPaid(eq(42L), sessionCaptor.capture());
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo("received");
            assertThat(sessionCaptor.getValue().getId()).isEqualTo("sess_123");
            assertThat(sessionCaptor.getValue().getPaymentIntent()).isEqualTo("pi_123");
            assertThat(sessionCaptor.getValue().getClientReferenceId()).isEqualTo("42");
        }
    }

    @Test
    @DisplayName("handleWebhook() ignore un paiement sans reference client")
    void handleWebhook_shouldIgnoreMissingClientReference() throws Exception {
        String payload = """
                {"data":{"object":{
                  "id":"sess_123",
                  "payment_intent":"pi_123"
                }}}
                """;
        Event event = new Event();
        event.setType("checkout.session.completed");

        try (MockedStatic<Webhook> webhook = mockStatic(Webhook.class)) {
            webhook.when(() -> Webhook.constructEvent(payload, "sig", "whsec_test"))
                    .thenReturn(event);

            ResponseEntity<String> response =
                    stripeWebhookController.handleWebhook(payload, "sig");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo("ignored");
            verify(commandeService, never()).markAsPaid(any(), any());
        }
    }

    @Test
    @DisplayName("handleWebhook() envoie l'email d'echec sur session expiree")
    void handleWebhook_shouldSendFailureEmailForExpiredSession() throws Exception {
        String payload = """
                {"data":{"object":{
                  "client_reference_id":"42"
                }}}
                """;
        Event event = new Event();
        event.setType("checkout.session.expired");

        try (MockedStatic<Webhook> webhook = mockStatic(Webhook.class)) {
            webhook.when(() -> Webhook.constructEvent(payload, "sig", "whsec_test"))
                    .thenReturn(event);

            ResponseEntity<String> response =
                    stripeWebhookController.handleWebhook(payload, "sig");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo("received");
            verify(commandeService).envoyerEmailEchecPaiement(42L);
        }
    }

    private Produit produit(Long id, String nom, double prix, int stock) {
        return Produit.builder()
                .id(id)
                .nom(nom)
                .description("Produit pour enfants")
                .prix(prix)
                .stock(stock)
                .imageUrl("/images/boutique/" + id + ".png")
                .seuilAlerte(2)
                .categorie(Categorie.builder()
                        .id(10L)
                        .nom("Jeux educatifs")
                        .build())
                .build();
    }

    private User user(Long id) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", id);
        user.setNom("Parent Test");
        user.setEmail("parent@example.com");
        user.setPassword("secret");
        return user;
    }

    private UserDetails userDetails(String email) {
        return org.springframework.security.core.userdetails.User
                .withUsername(email)
                .password("secret")
                .authorities("ROLE_PARENT")
                .build();
    }
}
