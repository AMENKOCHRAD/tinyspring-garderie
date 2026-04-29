package com.tinyspring.garderie.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import com.tinyspring.garderie.entity.Salle;
import com.tinyspring.garderie.interfaces.ISalleService;

import java.util.List;

@RestController
@RequestMapping("/api/salles")
public class SalleController {

    private final ISalleService salleService;

    public SalleController(ISalleService salleService) {
        this.salleService = salleService;
    }

    @PostMapping
    public Salle addSalle(@Valid @RequestBody Salle salle) {
        return salleService.addSalle(salle);
    }

    @PutMapping("/{id}")
    public Salle updateSalle(@PathVariable Long id, @Valid @RequestBody Salle salle) {
        salle.setId(id);
        return salleService.updateSalle(salle);
    }

    @DeleteMapping("/{id}")
    public void deleteSalle(@PathVariable Long id) {
        salleService.deleteSalle(id);
    }

    @GetMapping("/{id}")
    public Salle getSalleById(@PathVariable Long id) {
        return salleService.getSalleById(id);
    }

    @GetMapping
    public List<Salle> getAllSalles() {
        return salleService.getAllSalles();
    }
}
