package com.tinyspring.garderie.service.boutique;

import com.tinyspring.garderie.dto.boutique.*;
import com.tinyspring.garderie.entity.boutique.*;
import com.tinyspring.garderie.entity.User;
import com.tinyspring.garderie.mapper.boutique.CommandeMapper;
import com.tinyspring.garderie.repository.UserRepository;
import com.tinyspring.garderie.repository.boutique.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests CommandeService")
class CommandeServiceTest {

    @Mock private CommandeRepository commandeRepository;
    @Mock private CommandeProduitRepository commandeProduitRepository;
    @Mock private ProduitRepository produitRepository;
    @Mock private UserRepository userRepository;
    @Mock private StripeService stripeService;
    @Mock private EmailService emailService;
    @Mock private CommandeMapper commandeMapper;

    @InjectMocks
    private CommandeServiceImpl commandeService;

    private User user;
    private Produit produit;
    private Commande commande;
    private CommandeDto commandeDto;

    @BeforeEach
    void setUp() {
        user = new User("Parent Test", "parent@test.com", "password123", true, null);

        produit = Produit.builder()
                .id(9L).nom("Le Petit Prince")
                .prix(15.99).stock(10)
                .build();

        commande = Commande.builder()
                .id(1L).statut("PENDING")
                .paymentStatus("PENDING")
                .montantTotal(15.99)
                .user(user)
                .build();
        commande.setItems(new ArrayList<>());

        commandeDto = CommandeDto.builder()
                .id(1L).statut("PENDING")
                .paymentStatus("PENDING")
                .montantTotal(15.99)
                .userId(1L)
                .build();
    }

    // ── findAll ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll() retourne liste vide si aucune commande")
    void findAll_shouldReturnEmpty_whenNoCommandes() {
        when(commandeRepository.findAll()).thenReturn(List.of());

        List<CommandeDto> result = commandeService.findAll();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAll() retourne les commandes avec items")
    void findAll_shouldReturnCommandes() {
        when(commandeRepository.findAll()).thenReturn(List.of(commande));
        when(commandeRepository.findAllWithItems(List.of(1L)))
                .thenReturn(List.of(commande));
        when(commandeMapper.toDto(commande)).thenReturn(commandeDto);

        List<CommandeDto> result = commandeService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatut()).isEqualTo("PENDING");
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById() retourne la commande si elle existe")
    void findById_shouldReturnCommande_whenExists() {
        when(commandeRepository.findAllWithItems(List.of(1L)))
                .thenReturn(List.of(commande));
        when(commandeMapper.toDto(commande)).thenReturn(commandeDto);

        CommandeDto result = commandeService.findById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("findById() lève RuntimeException si commande introuvable")
    void findById_shouldThrow_whenNotFound() {
        when(commandeRepository.findAllWithItems(List.of(99L)))
                .thenReturn(List.of());

        assertThatThrownBy(() -> commandeService.findById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    // ── findByUser ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByUser() retourne liste vide si user sans commandes")
    void findByUser_shouldReturnEmpty_whenNoCommandes() {
        when(commandeRepository.findByUserId(1L)).thenReturn(List.of());

        List<CommandeDto> result = commandeService.findByUser(1L);

        assertThat(result).isEmpty();
    }

    // ── findByStatut ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByStatut() retourne liste vide si aucune commande avec ce statut")
    void findByStatut_shouldReturnEmpty_whenNoMatch() {
        when(commandeRepository.findByStatut("LIVREE")).thenReturn(List.of());

        List<CommandeDto> result = commandeService.findByStatut("LIVREE");

        assertThat(result).isEmpty();
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create() lève RuntimeException si panier vide")
    void create_shouldThrow_whenPanierVide() {
        CommandeRequest request = new CommandeRequest();
        request.setUserId(1L);
        request.setItems(List.of());

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> commandeService.create(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("panier");
    }

    @Test
    @DisplayName("create() lève RuntimeException si utilisateur introuvable")
    void create_shouldThrow_whenUserNotFound() {
        CommandeRequest request = new CommandeRequest();
        request.setUserId(99L);
        request.setItems(List.of(new CommandeItemRequest(9L, 1)));

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commandeService.create(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("create() lève RuntimeException si stock insuffisant")
    void create_shouldThrow_whenStockInsuffisant() {
        produit.setStock(1);

        CommandeRequest request = new CommandeRequest();
        request.setUserId(1L);
        request.setAdresseLivraison("123 rue Test");
        request.setItems(List.of(new CommandeItemRequest(9L, 5)));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(produitRepository.findById(9L)).thenReturn(Optional.of(produit));

        assertThatThrownBy(() -> commandeService.create(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Stock insuffisant");
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete() supprime la commande si elle existe")
    void delete_shouldDelete_whenExists() {
        when(commandeRepository.existsById(1L)).thenReturn(true);

        commandeService.delete(1L);

        verify(commandeRepository).deleteById(1L);
    }

    @Test
    @DisplayName("delete() lève RuntimeException si commande introuvable")
    void delete_shouldThrow_whenNotFound() {
        when(commandeRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> commandeService.delete(99L))
                .isInstanceOf(RuntimeException.class);

        verify(commandeRepository, never()).deleteById(any());
    }

    // ── updateStatut ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateStatut() PENDING → ANNULEE est autorisé")
    void updateStatut_pendingToAnnulee_shouldWork() {
        commande.setStatut("PENDING");
        commande.setPaymentStatus("PENDING");

        when(commandeRepository.findById(1L)).thenReturn(Optional.of(commande));
        when(commandeRepository.save(any())).thenReturn(commande);
        when(commandeMapper.toDto(any())).thenReturn(commandeDto);

        CommandeDto result = commandeService.updateStatut(1L, "ANNULEE");

        assertThat(result).isNotNull();
        verify(commandeRepository).save(any());
    }

    @Test
    @DisplayName("updateStatut() transition interdite lève RuntimeException")
    void updateStatut_invalidTransition_shouldThrow() {
        commande.setStatut("LIVREE");

        when(commandeRepository.findById(1L)).thenReturn(Optional.of(commande));

        assertThatThrownBy(() -> commandeService.updateStatut(1L, "PENDING"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Transition interdite");
    }

    // ── accepterPaiementEspeces ───────────────────────────────────────────────

    @Test
    @DisplayName("accepterPaiementEspeces() lève RuntimeException si token invalide")
    void accepterEspeces_shouldThrow_whenTokenInvalid() {
        when(commandeRepository.findByTokenAction("bad-token"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> commandeService.accepterPaiementEspeces("bad-token"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("invalide");
    }

    @Test
    @DisplayName("accepterPaiementEspeces() lève RuntimeException si commande annulée")
    void accepterEspeces_shouldThrow_whenCommandeAnnulee() {
        commande.setStatut("ANNULEE");
        when(commandeRepository.findByTokenAction("token123"))
                .thenReturn(Optional.of(commande));

        assertThatThrownBy(() -> commandeService.accepterPaiementEspeces("token123"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("annulée");
    }

    // ── refuserEtAnnuler ──────────────────────────────────────────────────────

    @Test
    @DisplayName("refuserEtAnnuler() annule la commande si token valide")
    void refuserEtAnnuler_shouldAnnuler_whenTokenValid() {
        commande.setStatut("PENDING");
        commande.setTokenAction("token123");

        when(commandeRepository.findByTokenAction("token123"))
                .thenReturn(Optional.of(commande));
        when(commandeRepository.save(any())).thenReturn(commande);

        commandeService.refuserEtAnnuler("token123");

        assertThat(commande.getStatut()).isEqualTo("ANNULEE");
        assertThat(commande.getTokenAction()).isNull();
        verify(commandeRepository).save(commande);
    }
}