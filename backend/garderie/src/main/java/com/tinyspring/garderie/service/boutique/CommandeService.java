package com.tinyspring.garderie.service.boutique;

import com.tinyspring.garderie.dto.boutique.CommandeDto;
import com.tinyspring.garderie.dto.boutique.CommandeRequest;
import com.tinyspring.garderie.entity.boutique.Commande;
import com.tinyspring.garderie.entity.boutique.Produit;
import com.tinyspring.garderie.entity.User;
import com.tinyspring.garderie.repository.UserRepository;
import com.tinyspring.garderie.repository.boutique.CommandeRepository;
import com.tinyspring.garderie.repository.boutique.ProduitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommandeService {

    private final CommandeRepository commandeRepository;
    private final ProduitRepository produitRepository;
    private final UserRepository userRepository;
    private final ProduitService produitService;

    public CommandeService(CommandeRepository commandeRepository,
                           ProduitRepository produitRepository,
                           UserRepository userRepository,
                           ProduitService produitService) {
        this.commandeRepository = commandeRepository;
        this.produitRepository = produitRepository;
        this.userRepository = userRepository;
        this.produitService = produitService;
    }

    // ── Mapping entité → DTO ──────────────────────────────────────────────────

    public CommandeDto toDto(Commande commande) {
        return CommandeDto.builder()
                .id(commande.getId())
                .dateCommande(commande.getDateCommande())
                .statut(commande.getStatut())
                .montantTotal(commande.getMontantTotal())
                .adresseLivraison(commande.getAdresseLivraison())
                .userId(commande.getUser().getId())
                .userNom(commande.getUser().getNom())
                .userEmail(commande.getUser().getEmail())
                .produits(commande.getProduits()
                        .stream()
                        .map(produitService::toDto)
                        .collect(Collectors.toList()))
                .build();
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    public List<CommandeDto> findAll() {
        return commandeRepository.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public CommandeDto findById(Long id) {
        Commande commande = commandeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Commande introuvable avec l'id : " + id));
        return toDto(commande);
    }

    public List<CommandeDto> findByUser(Long userId) {
        return commandeRepository.findByUserId(userId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<CommandeDto> findByStatut(String statut) {
        return commandeRepository.findByStatut(statut)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public CommandeDto create(CommandeRequest request) {
        // Récupérer l'utilisateur
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable avec l'id : " + request.getUserId()));

        // Récupérer les produits
        List<Produit> produits = produitRepository.findAllById(request.getProduitIds());
        if (produits.isEmpty()) {
            throw new RuntimeException("Aucun produit valide trouvé pour cette commande");
        }

        // Calculer le montant total
        double montantTotal = produits.stream()
                .mapToDouble(Produit::getPrix)
                .sum();

        Commande commande = Commande.builder()
                .adresseLivraison(request.getAdresseLivraison())
                .montantTotal(montantTotal)
                .user(user)
                .produits(produits)
                .build();

        return toDto(commandeRepository.save(commande));
    }


    @Transactional
    public CommandeDto updateStatut(Long id, String nouveauStatut) {
        Commande commande = commandeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Commande introuvable avec l'id : " + id));

        if ("CONFIRMEE".equals(nouveauStatut) && !"CONFIRMEE".equals(commande.getStatut())) {
            for (Produit produit : commande.getProduits()) {
                // Recharger le produit FRAIS depuis la DB pour avoir le vrai stock actuel
                Produit produitFrais = produitRepository.findById(produit.getId())
                        .orElseThrow(() -> new RuntimeException(
                                "Produit introuvable : " + produit.getId()));

                if (produitFrais.getStock() <= 0) {
                    throw new RuntimeException(
                            "Stock insuffisant pour le produit : \"" + produitFrais.getNom() + "\"" +
                                    " (stock actuel : " + produitFrais.getStock() + ")"
                    );
                }
                produitFrais.setStock(produitFrais.getStock() - 1);
                produitRepository.saveAndFlush(produitFrais);
            }
        }

        commande.setStatut(nouveauStatut);
        return toDto(commandeRepository.save(commande));
    }

    public void delete(Long id) {
        if (!commandeRepository.existsById(id)) {
            throw new RuntimeException("Commande introuvable avec l'id : " + id);
        }
        commandeRepository.deleteById(id);
    }
}
