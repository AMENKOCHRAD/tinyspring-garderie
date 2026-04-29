package com.tinyspring.garderie.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import com.tinyspring.garderie.entity.Groupe;
import com.tinyspring.garderie.interfaces.IGroupeService;

import java.util.List;

@RestController
@RequestMapping("/api/groupes")
public class GroupeController {

    private final IGroupeService groupeService;

    public GroupeController(IGroupeService groupeService) {
        this.groupeService = groupeService;
    }

    @PostMapping
    public Groupe addGroupe(@Valid @RequestBody Groupe groupe) {
        return groupeService.addGroupe(groupe);
    }

    @PutMapping("/{id}")
    public Groupe updateGroupe(@PathVariable Long id, @Valid @RequestBody Groupe groupe) {
        groupe.setId(id);
        return groupeService.updateGroupe(groupe);
    }

    @DeleteMapping("/{id}")
    public void deleteGroupe(@PathVariable Long id) {
        groupeService.deleteGroupe(id);
    }

    @GetMapping("/{id}")
    public Groupe getGroupeById(@PathVariable Long id) {
        return groupeService.getGroupeById(id);
    }

    @GetMapping
    public List<Groupe> getAllGroupes() {
        return groupeService.getAllGroupes();
    }

    @PostMapping("/suggestions")
    public List<com.tinyspring.garderie.dto.GroupeSuggestionDTO> suggestGroupes(@RequestBody com.tinyspring.garderie.dto.ChildMatchingRequest request) {
        return groupeService.suggestGroups(request);
    }
}
