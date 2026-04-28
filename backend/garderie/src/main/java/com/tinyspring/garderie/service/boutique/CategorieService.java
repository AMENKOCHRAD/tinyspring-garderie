package com.tinyspring.garderie.service.boutique;

import com.tinyspring.garderie.dto.boutique.CategorieDto;
import java.util.List;

public interface CategorieService {
    List<CategorieDto> findAll();
    CategorieDto findById(Long id);
    CategorieDto create(CategorieDto dto);
    CategorieDto update(Long id, CategorieDto dto);
    void delete(Long id);
}