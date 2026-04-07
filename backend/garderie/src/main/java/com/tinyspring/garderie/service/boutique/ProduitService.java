package com.tinyspring.garderie.service.boutique;

import com.tinyspring.garderie.dto.boutique.ProduitDto;
import com.tinyspring.garderie.entity.boutique.Categorie;
import com.tinyspring.garderie.entity.boutique.Commande;
import com.tinyspring.garderie.entity.boutique.Produit;
import com.tinyspring.garderie.repository.boutique.CategorieRepository;
import com.tinyspring.garderie.repository.boutique.CommandeRepository;
import com.tinyspring.garderie.repository.boutique.ProduitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProduitService {

    private final ProduitRepository produitRepository;
    private final CategorieRepository categorieRepository;
    private final CommandeRepository commandeRepository;

    public ProduitService(ProduitRepository produitRepository,
                          CategorieRepository categorieRepository,
                          CommandeRepository commandeRepository) {
        this.produitRepository = produitRepository;
        this.categorieRepository = categorieRepository;
        this.commandeRepository = commandeRepository;
    }

    // ── Mapping entité → DTO ──────────────────────────────────────────────────

    public ProduitDto toDto(Produit produit) {
        return ProduitDto.builder()
                .id(produit.getId())
                .nom(produit.getNom())
                .description(produit.getDescription())
                .prix(produit.getPrix())
                .stock(produit.getStock())
                .imageUrl(produit.getImageUrl())
                .categorieId(produit.getCategorie().getId())
                .categorieNom(produit.getCategorie().getNom())
                .seuilAlerte(produit.getSeuilAlerte())
                .build();
    }

    // ── Mapping DTO → entité ──────────────────────────────────────────────────

    public Produit toEntity(ProduitDto dto) {
        Categorie categorie = categorieRepository.findById(dto.getCategorieId())
                .orElseThrow(() -> new RuntimeException("Catégorie introuvable avec l'id : " + dto.getCategorieId()));
        return Produit.builder()
                .nom(dto.getNom())
                .description(dto.getDescription())
                .prix(dto.getPrix())
                .stock(dto.getStock())
                .imageUrl(dto.getImageUrl())
                .categorie(categorie)
                .build();
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    public List<ProduitDto> findAll() {
        return produitRepository.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public ProduitDto findById(Long id) {
        Produit produit = produitRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produit introuvable avec l'id : " + id));
        return toDto(produit);
    }

    public List<ProduitDto> findByCategorie(Long categorieId) {
        return produitRepository.findByCategorieId(categorieId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<ProduitDto> search(String nom) {
        return produitRepository.findByNomContainingIgnoreCase(nom)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<ProduitDto> findEnStock() {
        return produitRepository.findByStockGreaterThan(0)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public ProduitDto create(ProduitDto dto) {
        Produit saved = produitRepository.save(toEntity(dto));
        return toDto(saved);
    }

    public ProduitDto update(Long id, ProduitDto dto) {
        Produit existing = produitRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produit introuvable avec l'id : " + id));

        Categorie categorie = categorieRepository.findById(dto.getCategorieId())
                .orElseThrow(() -> new RuntimeException("Catégorie introuvable avec l'id : " + dto.getCategorieId()));

        existing.setNom(dto.getNom());
        existing.setDescription(dto.getDescription());
        existing.setPrix(dto.getPrix());
        existing.setStock(dto.getStock());
        existing.setImageUrl(dto.getImageUrl());
        existing.setCategorie(categorie);

        return toDto(produitRepository.save(existing));
    }

    // ── Vérifie si le produit est lié à des commandes ─────────────────────────
    public boolean hasCommandes(Long id) {
        return produitRepository.existsInCommandes(id);
    }

    public List<ProduitDto> findLowStock() {
        return produitRepository.findAll()
                .stream()
                .filter(p -> p.getStock() <= p.getSeuilAlerte())
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ── Suppression : détache d'abord de toutes les commandes ─────────────────
    @Transactional
    public void delete(Long id) {
        Produit produit = produitRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produit introuvable avec l'id : " + id));

        // Détacher le produit de toutes les commandes liées
        // pour éviter la contrainte FK sur commande_produit
        List<Commande> commandes = commandeRepository.findAll()
                .stream()
                .filter(c -> c.getProduits().stream().anyMatch(p -> p.getId().equals(id)))
                .collect(Collectors.toList());

        for (Commande commande : commandes) {
            commande.getProduits().removeIf(p -> p.getId().equals(id));
            commandeRepository.save(commande);
        }

        produitRepository.delete(produit);
    }
}
