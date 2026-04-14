package com.tinyspring.garderie.service.boutique;

import com.stripe.model.checkout.Session;
import com.tinyspring.garderie.dto.boutique.*;
import com.tinyspring.garderie.entity.boutique.Commande;
import com.tinyspring.garderie.entity.boutique.CommandeProduit;
import com.tinyspring.garderie.entity.boutique.Produit;
import com.tinyspring.garderie.entity.User;
import com.tinyspring.garderie.repository.UserRepository;
import com.tinyspring.garderie.repository.boutique.CommandeProduitRepository;
import com.tinyspring.garderie.repository.boutique.CommandeRepository;
import com.tinyspring.garderie.repository.boutique.ProduitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CommandeService {

    private final CommandeRepository commandeRepository;
    private final CommandeProduitRepository commandeProduitRepository;
    private final ProduitRepository produitRepository;
    private final UserRepository userRepository;
    private final StripeService stripeService;

    // ✅ EmailService retiré du constructeur obligatoire
    // → l'injecter en Optional ou créer un EmailService vide si pas encore implémenté

    public CommandeService(CommandeRepository commandeRepository,
                           CommandeProduitRepository commandeProduitRepository,
                           ProduitRepository produitRepository,
                           UserRepository userRepository,
                           StripeService stripeService) {
        this.commandeRepository = commandeRepository;
        this.commandeProduitRepository = commandeProduitRepository;
        this.produitRepository = produitRepository;
        this.userRepository = userRepository;
        this.stripeService = stripeService;
    }

    // ── Mapping entité → DTO ──────────────────────────────────────────────────

    public CommandeDto toDto(Commande commande) {
        List<CommandeItemDto> itemDtos = commande.getItems().stream()
                .map(item -> CommandeItemDto.builder()
                        .produitId(item.getProduit().getId())
                        .produitNom(item.getProduit().getNom())
                        .produitImageUrl(item.getProduit().getImageUrl())
                        .quantite(item.getQuantite())
                        .prixUnitaire(item.getPrixUnitaire())
                        .sousTotal(item.getSousTotal())
                        .build())
                .collect(Collectors.toList());

        return CommandeDto.builder()
                .id(commande.getId())
                .dateCommande(commande.getDateCommande())
                .statut(commande.getStatut())
                .paymentStatus(commande.getPaymentStatus())
                .montantTotal(commande.getMontantTotal())
                .adresseLivraison(commande.getAdresseLivraison())
                .stripeSessionId(commande.getStripeSessionId())
                .stripePaymentIntentId(commande.getStripePaymentIntentId())
                .userId(commande.getUser().getId())
                .userNom(commande.getUser().getNom())
                .userEmail(commande.getUser().getEmail())
                .items(itemDtos)
                .build();
    }

    // ── CRUD lecture ──────────────────────────────────────────────────────────

    public List<CommandeDto> findAll() {
        return commandeRepository.findAll()
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public CommandeDto findById(Long id) {
        return toDto(commandeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Commande introuvable : " + id)));
    }

    public List<CommandeDto> findByUser(Long userId) {
        return commandeRepository.findByUserId(userId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<CommandeDto> findByStatut(String statut) {
        return commandeRepository.findByStatut(statut)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    // ── ÉTAPE 1 : Créer commande (statut PENDING, stock NON décrémenté) ───────

    @Transactional
    public CommandeDto create(CommandeRequest request) {


        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException(
                        "Utilisateur introuvable : " + request.getUserId()));

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("Le panier est vide");
        }

        // Vérifier le stock de chaque produit avant de créer la commande
        for (CommandeItemRequest itemReq : request.getItems()) {
            Produit produit = produitRepository.findById(itemReq.getProduitId())
                    .orElseThrow(() -> new RuntimeException(
                            "Produit introuvable : " + itemReq.getProduitId()));

            if (produit.getStock() < itemReq.getQuantite()) {
                throw new RuntimeException(
                        "Stock insuffisant pour \"" + produit.getNom() + "\""
                                + " (disponible : " + produit.getStock()
                                + ", demandé : " + itemReq.getQuantite() + ")");
            }
        }

        // Calculer montant total
        double montantTotal = 0.0;
        for (CommandeItemRequest itemReq : request.getItems()) {
            Produit produit = produitRepository.findById(itemReq.getProduitId()).get();
            montantTotal += produit.getPrix() * itemReq.getQuantite();
        }

        // Créer la commande en PENDING
        Commande commande = Commande.builder()
                .adresseLivraison(request.getAdresseLivraison())
                .montantTotal(montantTotal)
                .statut("PENDING")
                .paymentStatus("PENDING")
                .user(user)
                .build();

        commande = commandeRepository.save(commande);

        // Créer les items avec snapshot du prix
        for (CommandeItemRequest itemReq : request.getItems()) {
            Produit produit = produitRepository.findById(itemReq.getProduitId()).get();
            CommandeProduit cp = CommandeProduit.builder()
                    .commande(commande)
                    .produit(produit)
                    .quantite(itemReq.getQuantite())
                    .prixUnitaire(produit.getPrix()) // snapshot prix actuel
                    .build();
            commandeProduitRepository.save(cp);
            commande.getItems().add(cp);
        }

        return toDto(commande);
    }

    // ── ÉTAPE 2 : Créer session Stripe et sauvegarder l'URL ──────────────────

    @Transactional
    public Map<String, String> createCheckoutSession(Long commandeId) {
        Commande commande = commandeRepository.findById(commandeId)
                .orElseThrow(() -> new RuntimeException("Commande introuvable : " + commandeId));

        if (!"PENDING".equals(commande.getStatut())) {
            throw new RuntimeException(
                    "La commande n'est pas en attente de paiement (statut : "
                            + commande.getStatut() + ")");
        }

        // ✅ Passer les items (avec quantités) au StripeService
        String checkoutUrl = stripeService.createCheckoutSession(commande, commande.getItems());

        return Map.of("checkoutUrl", checkoutUrl);
    }

    // ── ÉTAPE 3 : Webhook Stripe → paiement confirmé ─────────────────────────
    // Appelé par StripeWebhookController après vérification signature

    @Transactional
    public void markAsPaid(Long commandeId, Session session) {
        Commande commande = commandeRepository.findById(commandeId)
                .orElseThrow(() -> new RuntimeException("Commande introuvable : " + commandeId));

        // Sauvegarder infos Stripe
        commande.setStripeSessionId(session.getId());
        commande.setStripePaymentIntentId(session.getPaymentIntent());
        commande.setPaymentStatus("PAID");
        commande.setStatut("CONFIRMEE"); // ✅ Stripe confirme → CONFIRMEE auto

        // ✅ Décrémenter le stock ICI (pas dans updateStatut)
        for (CommandeProduit cp : commande.getItems()) {
            Produit produit = produitRepository.findById(cp.getProduit().getId())
                    .orElseThrow(() -> new RuntimeException(
                            "Produit introuvable : " + cp.getProduit().getId()));
            int newStock = Math.max(0, produit.getStock() - cp.getQuantite());
            produit.setStock(newStock);
            produitRepository.save(produit);
        }

        commandeRepository.save(commande);
    }

    // ── ÉTAPE 4 : Admin change le statut manuellement ─────────────────────────
    // Transitions autorisées : CONFIRMEE → EXPEDIEE → LIVREE
    //                          Tout → ANNULEE (avec remboursement si CONFIRMEE)

    @Transactional
    public CommandeDto updateStatut(Long id, String nouveauStatut) {
        Commande commande = commandeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Commande introuvable : " + id));

        // ✅ Valider la transition de statut
        validerTransition(commande.getStatut(), nouveauStatut);

        // ✅ Si annulation après paiement → rembourser + remettre le stock
        if ("ANNULEE".equals(nouveauStatut)
                && "PAID".equals(commande.getPaymentStatus())
                && commande.getStripePaymentIntentId() != null) {

            stripeService.createRefund(commande.getStripePaymentIntentId());
            restituerStock(commande);
            commande.setPaymentStatus("CANCELED");
        }

        commande.setStatut(nouveauStatut);
        return toDto(commandeRepository.save(commande));
    }

    // ── Supprimer une commande ────────────────────────────────────────────────

    public void delete(Long id) {
        if (!commandeRepository.existsById(id)) {
            throw new RuntimeException("Commande introuvable : " + id);
        }
        commandeRepository.deleteById(id);
    }

    // ── Helpers privés ────────────────────────────────────────────────────────

    private void validerTransition(String statutActuel, String nouveauStatut) {
        Map<String, List<String>> transitions = Map.of(
                "PENDING",    List.of("ANNULEE"),
                "CONFIRMEE",  List.of("EXPEDIEE", "ANNULEE"),
                "EXPEDIEE",   List.of("LIVREE", "ANNULEE"),
                "LIVREE",     List.of(),   // verrouillé
                "ANNULEE",    List.of()    // verrouillé
        );

        List<String> autorisees = transitions.getOrDefault(statutActuel, List.of());

        if (!autorisees.contains(nouveauStatut)) {
            throw new RuntimeException(
                    "Transition interdite : " + statutActuel + " → " + nouveauStatut
                            + ". Transitions autorisées : " + autorisees);
        }
    }

    private void restituerStock(Commande commande) {
        for (CommandeProduit cp : commande.getItems()) {
            Produit produit = produitRepository.findById(cp.getProduit().getId())
                    .orElseThrow();
            produit.setStock(produit.getStock() + cp.getQuantite());
            produitRepository.save(produit);
        }
    }
}
