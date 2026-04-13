package com.tinyspring.garderie.controller.Classes;

import com.tinyspring.garderie.dto.Classes.ClasseOptionResponse;
import com.tinyspring.garderie.repository.Classes.ClasseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
public class ClasseRestController {
    private final ClasseRepository classeRepository;

    @GetMapping
    public ResponseEntity<List<ClasseOptionResponse>> getAll() {
        List<ClasseOptionResponse> classes = classeRepository
                .findAll(Sort.by(Sort.Order.asc("niveau"), Sort.Order.asc("nom")))
                .stream()
                .map(classe -> ClasseOptionResponse.builder()
                        .id(classe.getId())
                        .nom(classe.getNom())
                        .niveau(classe.getNiveau())
                        .build())
                .toList();

        return ResponseEntity.ok(classes);
    }
}
