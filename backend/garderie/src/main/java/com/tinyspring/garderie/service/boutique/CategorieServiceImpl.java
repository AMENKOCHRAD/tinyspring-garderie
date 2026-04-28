package com.tinyspring.garderie.service.boutique;

import com.tinyspring.garderie.dto.boutique.CategorieDto;
import com.tinyspring.garderie.entity.boutique.Categorie;
import com.tinyspring.garderie.mapper.boutique.CategorieMapper;
import com.tinyspring.garderie.repository.boutique.CategorieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategorieServiceImpl implements CategorieService {

    private final CategorieRepository categorieRepository;
    private final CategorieMapper categorieMapper;

    @Override
    public List<CategorieDto> findAll() {
        return categorieRepository.findAll()
                .stream()
                .map(categorieMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public CategorieDto findById(Long id) {
        Categorie categorie = categorieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Catégorie introuvable avec l'id : " + id));
        return categorieMapper.toDto(categorie);
    }

    @Override
    public CategorieDto create(CategorieDto dto) {
        if (categorieRepository.existsByNom(dto.getNom())) {
            throw new RuntimeException(
                    "Une catégorie avec ce nom existe déjà : " + dto.getNom());
        }
        Categorie saved = categorieRepository.save(categorieMapper.toEntity(dto));
        return categorieMapper.toDto(saved);
    }

    @Override
    public CategorieDto update(Long id, CategorieDto dto) {
        Categorie existing = categorieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Catégorie introuvable avec l'id : " + id));
        existing.setNom(dto.getNom());
        existing.setDescription(dto.getDescription());
        existing.setImageUrl(dto.getImageUrl());
        return categorieMapper.toDto(categorieRepository.save(existing));
    }

    @Override
    public void delete(Long id) {
        if (!categorieRepository.existsById(id)) {
            throw new RuntimeException(
                    "Catégorie introuvable avec l'id : " + id);
        }
        categorieRepository.deleteById(id);
    }
}