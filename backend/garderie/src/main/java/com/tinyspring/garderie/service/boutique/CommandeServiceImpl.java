package com.tinyspring.garderie.service.boutique;

import com.stripe.model.checkout.Session;
import com.tinyspring.garderie.dto.boutique.*;
import com.tinyspring.garderie.entity.boutique.*;
import com.tinyspring.garderie.entity.User;
import com.tinyspring.garderie.mapper.boutique.CommandeMapper;
import com.tinyspring.garderie.repository.UserRepository;
import com.tinyspring.garderie.repository.boutique.*;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CommandeServiceImpl implements CommandeService {

    private final CommandeRepository commandeRepository;
    private final CommandeProduitRepository commandeProduitRepository;
    private final ProduitRepository produitRepository;
    private final UserRepository userRepository;
    private final StripeService stripeService;
    private final EmailService emailService;
    private final CommandeMapper commandeMapper;

    public CommandeServiceImpl(CommandeRepository commandeRepository,
                               CommandeProduitRepository commandeProduitRepository,
                               ProduitRepository produitRepository,
                               UserRepository userRepository,
                               StripeService stripeService,
                               @Lazy EmailService emailService,
                               CommandeMapper commandeMapper) {
        this.commandeRepository = commandeRepository;
        this.commandeProduitRepository = commandeProduitRepository;
        this.produitRepository = produitRepository;
        this.userRepository = userRepository;
        this.stripeService = stripeService;
        this.emailService = emailService;
        this.commandeMapper = commandeMapper;
    }

    @Override
    public List<CommandeDto> findAll() {
        List<Long> ids = commandeRepository.findAll()
                .stream().map(Commande::getId).toList();
        if (ids.isEmpty()) return List.of();
        return commandeRepository.findAllWithItems(ids).stream()
                .map(commandeMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public CommandeDto findById(Long id) {
        return commandeMapper.toDto(
                commandeRepository.findAllWithItems(List.of(id))
                        .stream().findFirst()
                        .orElseThrow(() -> new RuntimeException(
                                "Commande introuvable : " + id)));
    }

    @Override
    public List<CommandeDto> findByUser(Long userId) {
        List<Long> ids = commandeRepository.findByUserId(userId)
                .stream().map(Commande::getId).toList();
        if (ids.isEmpty()) return List.of();
        return commandeRepository.findAllWithItems(ids).stream()
                .map(commandeMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public List<CommandeDto> findByStatut(String statut) {
        List<Long> ids = commandeRepository.findByStatut(statut)
                .stream().map(Commande::getId).toList();
        if (ids.isEmpty()) return List.of();
        return commandeRepository.findAllWithItems(ids).stream()
                .map(commandeMapper::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CommandeDto create(CommandeRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException(
                        "Utilisateur introuvable : " + request.getUserId()));
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("Le panier est vide");
        }
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
        double montantTotal = 0.0;
        for (CommandeItemRequest itemReq : request.getItems()) {
            Produit produit = produitRepository.findById(
                    itemReq.getProduitId()).get();
            montantTotal += produit.getPrix() * itemReq.getQuantite();
        }
        Commande commande = Commande.builder()
                .adresseLivraison(request.getAdresseLivraison())
                .montantTotal(montantTotal)
                .statut("PENDING")
                .paymentStatus("PENDING")
                .user(user)
                .build();
        commande = commandeRepository.save(commande);
        for (CommandeItemRequest itemReq : request.getItems()) {
            Produit produit = produitRepository.findById(
                    itemReq.getProduitId()).get();
            CommandeProduit cp = CommandeProduit.builder()
                    .commande(commande)
                    .produit(produit)
                    .quantite(itemReq.getQuantite())
                    .prixUnitaire(produit.getPrix())
                    .build();
            commandeProduitRepository.save(cp);
            commande.getItems().add(cp);
        }
        return commandeMapper.toDto(commande);
    }

    @Override
    @Transactional
    public Map<String, String> createCheckoutSession(Long commandeId) {
        Commande commande = commandeRepository.findById(commandeId)
                .orElseThrow(() -> new RuntimeException(
                        "Commande introuvable : " + commandeId));
        if (!"PENDING".equals(commande.getStatut())) {
            throw new RuntimeException(
                    "La commande n'est pas en attente de paiement (statut : "
                            + commande.getStatut() + ")");
        }
        Map<String, String> stripeResult = stripeService
                .createCheckoutSession(commande, commande.getItems());
        commande.setStripeSessionId(stripeResult.get("sessionId"));
        commandeRepository.save(commande);
        return Map.of("checkoutUrl", stripeResult.get("url"));
    }

    @Override
    @Transactional
    public void markAsPaid(Long commandeId, Session session) {
        Commande commande = commandeRepository.findById(commandeId)
                .orElseThrow(() -> new RuntimeException(
                        "Commande introuvable : " + commandeId));
        commande.setStripeSessionId(session.getId());
        commande.setStripePaymentIntentId(session.getPaymentIntent());
        commande.setPaymentStatus("PAID");
        commande.setStatut("CONFIRMEE");
        for (CommandeProduit cp : commande.getItems()) {
            Produit produit = produitRepository.findById(cp.getProduit().getId())
                    .orElseThrow(() -> new RuntimeException(
                            "Produit introuvable : " + cp.getProduit().getId()));
            produit.setStock(Math.max(0, produit.getStock() - cp.getQuantite()));
            produitRepository.save(produit);
        }
        commandeRepository.save(commande);
    }

    @Override
    @Transactional
    public CommandeDto updateStatut(Long id, String nouveauStatut) {
        Commande commande = commandeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Commande introuvable : " + id));
        validerTransition(commande.getStatut(), nouveauStatut);
        if ("ANNULEE".equals(nouveauStatut)
                && "PAID".equals(commande.getPaymentStatus())
                && commande.getStripePaymentIntentId() != null) {
            stripeService.createRefund(commande.getStripePaymentIntentId());
            restituerStock(commande);
            commande.setPaymentStatus("CANCELED");
        }
        commande.setStatut(nouveauStatut);
        return commandeMapper.toDto(commandeRepository.save(commande));
    }

    @Override
    public void delete(Long id) {
        if (!commandeRepository.existsById(id)) {
            throw new RuntimeException("Commande introuvable : " + id);
        }
        commandeRepository.deleteById(id);
    }

    @Override
    public void envoyerEmailEchecPaiement(Long commandeId) {
        Commande commande = commandeRepository.findById(commandeId)
                .orElseThrow();
        String token = UUID.randomUUID().toString();
        commande.setTokenAction(token);
        commandeRepository.save(commande);
        String lienEspeces = "http://localhost:8081/api/boutique/commandes/action/especes/" + token;
        String lienRefus   = "http://localhost:8081/api/boutique/commandes/action/refuser/" + token;
        emailService.envoyerEmailEchecPaiement(commande, lienEspeces, lienRefus);
    }

    @Override
    @Transactional
    public void accepterPaiementEspeces(String token) {
        Commande commande = commandeRepository.findByTokenAction(token)
                .orElseThrow(() -> new RuntimeException("Lien invalide ou expiré"));
        if ("ANNULEE".equals(commande.getStatut())) {
            throw new RuntimeException("Commande déjà annulée");
        }
        commande.setModePaiement("ESPECES");
        commande.setPaiementEspecesDemande(true);
        commande.setDateChoixEspeces(LocalDateTime.now());
        commande.setTokenAction(null);
        commandeRepository.save(commande);
        emailService.notifierAdminPaiementEspeces(commande);
    }

    @Override
    @Transactional
    public void refuserEtAnnuler(String token) {
        Commande commande = commandeRepository.findByTokenAction(token)
                .orElseThrow(() -> new RuntimeException("Lien invalide ou expiré"));
        commande.setStatut("ANNULEE");
        commande.setTokenAction(null);
        commandeRepository.save(commande);
    }

    private void validerTransition(String statutActuel, String nouveauStatut) {
        Map<String, List<String>> transitions = Map.of(
                "PENDING",   List.of("ANNULEE"),
                "CONFIRMEE", List.of("EXPEDIEE", "ANNULEE"),
                "EXPEDIEE",  List.of("LIVREE", "ANNULEE"),
                "LIVREE",    List.of(),
                "ANNULEE",   List.of()
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