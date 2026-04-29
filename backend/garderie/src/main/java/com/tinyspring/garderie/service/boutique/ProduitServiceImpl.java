package com.tinyspring.garderie.service.boutique;

import com.tinyspring.garderie.dto.boutique.ProduitDto;
import com.tinyspring.garderie.entity.boutique.Categorie;
import com.tinyspring.garderie.entity.boutique.Commande;
import com.tinyspring.garderie.entity.boutique.Produit;
import com.tinyspring.garderie.mapper.boutique.ProduitMapper;
import com.tinyspring.garderie.repository.boutique.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProduitServiceImpl implements ProduitService {

    private final ProduitRepository produitRepository;
    private final CategorieRepository categorieRepository;
    private final CommandeRepository commandeRepository;
    private final CommandeProduitRepository commandeProduitRepository;
    private final ProduitMapper produitMapper;

    private Produit buildEntityWithCategorie(ProduitDto dto) {
        Categorie categorie = categorieRepository.findById(dto.getCategorieId())
                .orElseThrow(() -> new RuntimeException(
                        "Catégorie introuvable avec l'id : " + dto.getCategorieId()));
        Produit produit = produitMapper.toEntity(dto);
        produit.setCategorie(categorie);
        return produit;
    }

    @Override
    public List<ProduitDto> findAll() {
        return produitRepository.findAll().stream()
                .map(produitMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public ProduitDto findById(Long id) {
        return produitMapper.toDto(produitRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Produit introuvable avec l'id : " + id)));
    }

    @Override
    public List<ProduitDto> findByCategorie(Long categorieId) {
        return produitRepository.findByCategorieId(categorieId).stream()
                .map(produitMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public List<ProduitDto> search(String nom) {
        return produitRepository.findByNomContainingIgnoreCase(nom).stream()
                .map(produitMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public List<ProduitDto> findEnStock() {
        return produitRepository.findByStockGreaterThan(0).stream()
                .map(produitMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public List<ProduitDto> findLowStock() {
        return produitRepository.findAll().stream()
                .filter(p -> p.getStock() <= p.getSeuilAlerte())
                .map(produitMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public ProduitDto create(ProduitDto dto) {
        return produitMapper.toDto(
                produitRepository.save(buildEntityWithCategorie(dto)));
    }

    @Override
    public ProduitDto update(Long id, ProduitDto dto) {
        Produit existing = produitRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Produit introuvable avec l'id : " + id));
        Categorie categorie = categorieRepository.findById(dto.getCategorieId())
                .orElseThrow(() -> new RuntimeException(
                        "Catégorie introuvable avec l'id : " + dto.getCategorieId()));
        existing.setNom(dto.getNom());
        existing.setDescription(dto.getDescription());
        existing.setPrix(dto.getPrix());
        existing.setStock(dto.getStock());
        existing.setImageUrl(dto.getImageUrl());
        existing.setCategorie(categorie);
        return produitMapper.toDto(produitRepository.save(existing));
    }

    @Override
    public boolean hasCommandes(Long id) {
        return commandeProduitRepository.findAll().stream()
                .anyMatch(cp -> cp.getProduit().getId().equals(id));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Produit produit = produitRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Produit introuvable avec l'id : " + id));
        List<Commande> commandes = commandeRepository.findAll().stream()
                .filter(c -> c.getItems().stream()
                        .anyMatch(cp -> cp.getProduit().getId().equals(id)))
                .collect(Collectors.toList());
        for (Commande commande : commandes) {
            commande.getItems().removeIf(cp -> cp.getProduit().getId().equals(id));
            commandeRepository.save(commande);
        }
        produitRepository.delete(produit);
    }

    @Override
    public Page<ProduitDto> findAllPaginated(Pageable pageable) {
        return produitRepository.findAll(pageable)
                .map(produitMapper::toDto);
    }

    @Override
    public Page<ProduitDto> searchPaginated(String nom, Pageable pageable) {
        return produitRepository.findByNomContainingIgnoreCase(nom, pageable)
                .map(produitMapper::toDto);
    }

    @Override
    public Page<ProduitDto> findByCategoriePaginated(Long categorieId, Pageable pageable) {
        return produitRepository.findByCategorieId(categorieId, pageable)
                .map(produitMapper::toDto);
    }
}