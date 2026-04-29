package com.tinyspring.garderie.service.boutique;

import com.tinyspring.garderie.dto.boutique.ProduitDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProduitService {
    List<ProduitDto> findAll();
    ProduitDto findById(Long id);
    List<ProduitDto> findByCategorie(Long categorieId);
    List<ProduitDto> search(String nom);
    List<ProduitDto> findEnStock();
    List<ProduitDto> findLowStock();
    ProduitDto create(ProduitDto dto);
    ProduitDto update(Long id, ProduitDto dto);
    boolean hasCommandes(Long id);
    void delete(Long id);

    Page<ProduitDto> findAllPaginated(Pageable pageable);
    Page<ProduitDto> searchPaginated(String nom, Pageable pageable);
    Page<ProduitDto> findByCategoriePaginated(Long categorieId, Pageable pageable);
}