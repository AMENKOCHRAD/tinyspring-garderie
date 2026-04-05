package com.tinyspring.garderie.service.boutique;

import com.tinyspring.garderie.dto.boutique.CategorieDto;
import com.tinyspring.garderie.entity.boutique.Categorie;
import com.tinyspring.garderie.repository.boutique.CategorieRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategorieService {

    private final CategorieRepository categorieRepository;

    public CategorieService(CategorieRepository categorieRepository) {
        this.categorieRepository = categorieRepository;
    }

    // ── Mapping entité → DTO ──────────────────────────────────────────────────

    public CategorieDto toDto(Categorie categorie) {
        return CategorieDto.builder()
                .id(categorie.getId())
                .nom(categorie.getNom())
                .description(categorie.getDescription())
                .imageUrl(categorie.getImageUrl())
                .nombreProduits(categorie.getProduits() != null ? categorie.getProduits().size() : 0)
                .build();
    }

    // ── Mapping DTO → entité ──────────────────────────────────────────────────

    public Categorie toEntity(CategorieDto dto) {
        return Categorie.builder()
                .nom(dto.getNom())
                .description(dto.getDescription())
                .imageUrl(dto.getImageUrl())
                .build();
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    public List<CategorieDto> findAll() {
        return categorieRepository.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public CategorieDto findById(Long id) {
        Categorie categorie = categorieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Catégorie introuvable avec l'id : " + id));
        return toDto(categorie);
    }

    public CategorieDto create(CategorieDto dto) {
        if (categorieRepository.existsByNom(dto.getNom())) {
            throw new RuntimeException("Une catégorie avec ce nom existe déjà : " + dto.getNom());
        }
        Categorie saved = categorieRepository.save(toEntity(dto));
        return toDto(saved);
    }

    public CategorieDto update(Long id, CategorieDto dto) {
        Categorie existing = categorieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Catégorie introuvable avec l'id : " + id));
        existing.setNom(dto.getNom());
        existing.setDescription(dto.getDescription());
        existing.setImageUrl(dto.getImageUrl());
        return toDto(categorieRepository.save(existing));
    }

    public void delete(Long id) {
        if (!categorieRepository.existsById(id)) {
            throw new RuntimeException("Catégorie introuvable avec l'id : " + id);
        }
        categorieRepository.deleteById(id);
    }
}
