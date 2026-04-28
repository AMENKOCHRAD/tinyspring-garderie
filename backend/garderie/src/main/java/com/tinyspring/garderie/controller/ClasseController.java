package com.tinyspring.garderie.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import com.tinyspring.garderie.entity.Classe;
import com.tinyspring.garderie.interfaces.IClasseService;

import java.util.List;

@RestController
@RequestMapping("/api/classes")
public class ClasseController {

    private final IClasseService classeService;

    public ClasseController(IClasseService classeService) {
        this.classeService = classeService;
    }

    @PostMapping
    public Classe addClasse(@Valid @RequestBody Classe classe) {
        return classeService.addClasse(classe);
    }

    @PutMapping("/{id}")
    public Classe updateClasse(@PathVariable Long id, @Valid @RequestBody Classe classe) {
        classe.setId(id);
        return classeService.updateClasse(classe);
    }

    @DeleteMapping("/{id}")
    public void deleteClasse(@PathVariable Long id) {
        classeService.deleteClasse(id);
    }

    @GetMapping("/{id}")
    public Classe getClasseById(@PathVariable Long id) {
        return classeService.getClasseById(id);
    }

    @GetMapping
    public List<Classe> getAllClasses() {
        return classeService.getAllClasses();
    }
}
