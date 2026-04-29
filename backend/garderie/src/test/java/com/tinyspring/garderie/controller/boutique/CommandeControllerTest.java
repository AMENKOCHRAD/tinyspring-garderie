package com.tinyspring.garderie.controller.boutique;

import com.tinyspring.garderie.dto.boutique.CommandeDto;
import com.tinyspring.garderie.dto.boutique.CommandeItemRequest;
import com.tinyspring.garderie.dto.boutique.CommandeRequest;
import com.tinyspring.garderie.entity.boutique.Commande;
import com.tinyspring.garderie.repository.boutique.CommandeRepository;
import com.tinyspring.garderie.service.boutique.CommandeService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests CommandeController")
class CommandeControllerTest {

    @Mock
    private CommandeService commandeService;

    @Mock
    private CommandeRepository commandeRepository;

    @Mock
    private HttpServletResponse httpServletResponse;

    @InjectMocks
    private CommandeController commandeController;

    private CommandeDto commandeDto;
    private CommandeRequest commandeRequest;

    @BeforeEach
    void setUp() {
        commandeDto = CommandeDto.builder()
                .id(42L)
                .dateCommande(LocalDateTime.of(2026, 4, 29, 10, 30))
                .statut("PENDING")
                .paymentStatus("PENDING")
                .montantTotal(59.90)
                .adresseLivraison("12 rue des Fleurs")
                .stripeSessionId("sess_123")
                .userId(7L)
                .userNom("Parent Test")
                .userEmail("parent@example.com")
                .items(List.of())
                .build();

        commandeRequest = CommandeRequest.builder()
                .adresseLivraison("12 rue des Fleurs")
                .userId(7L)
                .items(List.of(CommandeItemRequest.builder()
                        .produitId(3L)
                        .quantite(2)
                        .build()))
                .build();
    }

    @Test
    @DisplayName("getAll() retourne 200 avec les commandes admin")
    void getAll_shouldReturn200WithData() {
        when(commandeService.findAll()).thenReturn(List.of(commandeDto));

        ResponseEntity<List<CommandeDto>> response = commandeController.getAll();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(commandeDto);
    }

    @Test
    @DisplayName("getByUser() retourne 200 avec les commandes utilisateur")
    void getByUser_shouldReturn200WithData() {
        when(commandeService.findByUser(7L)).thenReturn(List.of(commandeDto));

        ResponseEntity<List<CommandeDto>> response = commandeController.getByUser(7L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(commandeDto);
    }

    @Test
    @DisplayName("getById() retourne 200 quand la commande existe")
    void getById_shouldReturn200WhenFound() {
        when(commandeService.findById(42L)).thenReturn(commandeDto);

        ResponseEntity<?> response = commandeController.getById(42L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(commandeDto);
    }

    @Test
    @DisplayName("getById() retourne 404 quand la commande est introuvable")
    void getById_shouldReturn404WhenNotFound() {
        when(commandeService.findById(99L)).thenThrow(new RuntimeException("Commande introuvable"));

        ResponseEntity<?> response = commandeController.getById(99L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo(Map.of("message", "Commande introuvable"));
    }

    @Test
    @DisplayName("getByIdAdmin() retourne 200 quand la commande existe")
    void getByIdAdmin_shouldReturn200WhenFound() {
        when(commandeService.findById(42L)).thenReturn(commandeDto);

        ResponseEntity<?> response = commandeController.getByIdAdmin(42L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(commandeDto);
    }

    @Test
    @DisplayName("getByIdAdmin() retourne 404 quand la commande est introuvable")
    void getByIdAdmin_shouldReturn404WhenNotFound() {
        when(commandeService.findById(99L)).thenThrow(new RuntimeException("Commande introuvable"));

        ResponseEntity<?> response = commandeController.getByIdAdmin(99L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo(Map.of("message", "Commande introuvable"));
    }

    @Test
    @DisplayName("getByStatut() retourne 200 avec les commandes filtrees")
    void getByStatut_shouldReturn200WithData() {
        when(commandeService.findByStatut("PENDING")).thenReturn(List.of(commandeDto));

        ResponseEntity<List<CommandeDto>> response = commandeController.getByStatut("PENDING");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(commandeDto);
    }

    @Test
    @DisplayName("updateStatut() retourne 200 avec la commande mise a jour")
    void updateStatut_shouldReturn200WhenUpdated() {
        CommandeDto updated = CommandeDto.builder()
                .id(42L)
                .statut("CONFIRMEE")
                .items(List.of())
                .build();
        when(commandeService.updateStatut(42L, "CONFIRMEE")).thenReturn(updated);

        ResponseEntity<?> response = commandeController.updateStatut(42L, "CONFIRMEE");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(updated);
    }

    @Test
    @DisplayName("updateStatut() retourne 400 quand le service refuse")
    void updateStatut_shouldReturn400WhenServiceThrows() {
        when(commandeService.updateStatut(42L, "LIVREE"))
                .thenThrow(new RuntimeException("Transition invalide"));

        ResponseEntity<?> response = commandeController.updateStatut(42L, "LIVREE");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(Map.of("message", "Transition invalide"));
    }

    @Test
    @DisplayName("delete() retourne 204 quand la suppression reussit")
    void delete_shouldReturn204WhenDeleted() {
        ResponseEntity<?> response = commandeController.delete(42L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(commandeService).delete(42L);
    }

    @Test
    @DisplayName("delete() retourne 409 en cas de conflit d'integrite")
    void delete_shouldReturn409WhenIntegrityConflict() {
        doThrow(new DataIntegrityViolationException("linked"))
                .when(commandeService).delete(42L);

        ResponseEntity<?> response = commandeController.delete(42L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody())
                .isEqualTo(Map.of("message", "Impossible de supprimer cette commande."));
    }

    @Test
    @DisplayName("delete() retourne 500 quand une erreur inattendue survient")
    void delete_shouldReturn500WhenRuntimeExceptionOccurs() {
        doThrow(new RuntimeException("Base indisponible"))
                .when(commandeService).delete(42L);

        ResponseEntity<?> response = commandeController.delete(42L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo(Map.of("message", "Erreur : Base indisponible"));
    }

    @Test
    @DisplayName("getBySession() retourne 200 quand la session existe")
    void getBySession_shouldReturn200WhenFound() {
        Commande commande = Commande.builder()
                .id(42L)
                .statut("PENDING")
                .items(new ArrayList<>())
                .build();
        when(commandeRepository.findByStripeSessionId("sess_123"))
                .thenReturn(Optional.of(commande));
        when(commandeService.findById(42L)).thenReturn(commandeDto);

        ResponseEntity<CommandeDto> response = commandeController.getBySession("sess_123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(commandeDto);
    }

    @Test
    @DisplayName("getBySession() retourne 404 quand la session est absente")
    void getBySession_shouldReturn404WhenMissing() {
        when(commandeRepository.findByStripeSessionId("missing"))
                .thenReturn(Optional.empty());

        ResponseEntity<CommandeDto> response = commandeController.getBySession("missing");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

    @Test
    @DisplayName("create() retourne 201 avec la commande creee")
    void create_shouldReturn201WhenCreated() {
        when(commandeService.create(commandeRequest)).thenReturn(commandeDto);

        ResponseEntity<?> response = commandeController.create(commandeRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(commandeDto);
    }

    @Test
    @DisplayName("create() retourne 400 quand la creation echoue")
    void create_shouldReturn400WhenServiceThrows() {
        when(commandeService.create(commandeRequest))
                .thenThrow(new RuntimeException("Stock insuffisant"));

        ResponseEntity<?> response = commandeController.create(commandeRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(Map.of("message", "Stock insuffisant"));
    }

    @Test
    @DisplayName("createCheckoutSession() retourne 200 avec l'URL Stripe")
    void createCheckoutSession_shouldReturn200WithCheckoutUrl() {
        Map<String, String> checkout = Map.of("checkoutUrl", "https://stripe.test/session");
        when(commandeService.createCheckoutSession(42L)).thenReturn(checkout);

        ResponseEntity<?> response = commandeController.createCheckoutSession(42L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(checkout);
    }

    @Test
    @DisplayName("createCheckoutSession() retourne 400 quand Stripe echoue")
    void createCheckoutSession_shouldReturn400WhenServiceThrows() {
        when(commandeService.createCheckoutSession(42L))
                .thenThrow(new RuntimeException("Session impossible"));

        ResponseEntity<?> response = commandeController.createCheckoutSession(42L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(Map.of("message", "Session impossible"));
    }

    @Test
    @DisplayName("accepterEspeces() redirige vers la confirmation")
    void accepterEspeces_shouldRedirectToConfirmation() throws IOException {
        commandeController.accepterEspeces("token-ok", httpServletResponse);

        verify(commandeService).accepterPaiementEspeces("token-ok");
        verify(httpServletResponse)
                .sendRedirect("http://localhost:4200/parent/boutique/paiement/especes-confirme");
    }

    @Test
    @DisplayName("accepterEspeces() redirige vers lien expire en cas d'erreur")
    void accepterEspeces_shouldRedirectToExpiredLinkWhenServiceThrows() throws IOException {
        doThrow(new RuntimeException("expire"))
                .when(commandeService).accepterPaiementEspeces("token-ko");

        commandeController.accepterEspeces("token-ko", httpServletResponse);

        verify(httpServletResponse)
                .sendRedirect("http://localhost:4200/parent/boutique/paiement/lien-expire");
    }

    @Test
    @DisplayName("refuserCommande() redirige vers la page d'annulation")
    void refuserCommande_shouldRedirectToCancellation() throws IOException {
        commandeController.refuserCommande("token-ok", httpServletResponse);

        verify(commandeService).refuserEtAnnuler("token-ok");
        verify(httpServletResponse)
                .sendRedirect("http://localhost:4200/parent/boutique/paiement/commande-annulee");
    }

    @Test
    @DisplayName("refuserCommande() redirige vers lien expire en cas d'erreur")
    void refuserCommande_shouldRedirectToExpiredLinkWhenServiceThrows() throws IOException {
        doThrow(new RuntimeException("expire"))
                .when(commandeService).refuserEtAnnuler("token-ko");

        commandeController.refuserCommande("token-ko", httpServletResponse);

        verify(httpServletResponse)
                .sendRedirect("http://localhost:4200/parent/boutique/paiement/lien-expire");
    }

    @Test
    @DisplayName("signalerEchec() envoie l'email quand la commande est pending")
    void signalerEchec_shouldSendEmailWhenCommandeIsPending() {
        Commande commande = Commande.builder()
                .id(42L)
                .statut("PENDING")
                .items(new ArrayList<>())
                .build();
        when(commandeRepository.findByStripeSessionId("sess_123"))
                .thenReturn(Optional.of(commande));

        ResponseEntity<?> response = commandeController.signalerEchec("sess_123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("ok");
        verify(commandeService).envoyerEmailEchecPaiement(42L);
    }

    @Test
    @DisplayName("signalerEchec() ignore les commandes non pending")
    void signalerEchec_shouldNotSendEmailWhenCommandeIsNotPending() {
        Commande commande = Commande.builder()
                .id(42L)
                .statut("PAID")
                .items(new ArrayList<>())
                .build();
        when(commandeRepository.findByStripeSessionId("sess_123"))
                .thenReturn(Optional.of(commande));

        ResponseEntity<?> response = commandeController.signalerEchec("sess_123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("ok");
        verify(commandeService, never()).envoyerEmailEchecPaiement(42L);
    }

    @Test
    @DisplayName("signalerEchec() retourne ignored quand le repository echoue")
    void signalerEchec_shouldReturnIgnoredWhenRepositoryThrows() {
        when(commandeRepository.findByStripeSessionId("sess_123"))
                .thenThrow(new RuntimeException("db"));

        ResponseEntity<?> response = commandeController.signalerEchec("sess_123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("ignored");
    }
}
