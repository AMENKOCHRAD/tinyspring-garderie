package com.tinyspring.garderie.controller.boutique;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinyspring.garderie.entity.boutique.Produit;
import com.tinyspring.garderie.repository.boutique.CommandeProduitRepository;
import com.tinyspring.garderie.repository.boutique.ProduitRepository;
import com.tinyspring.garderie.service.boutique.OllamaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/marketing")
@CrossOrigin(origins = {"http://localhost:4200",
        "http://localhost:21065"})
public class MarketingAdvisorController {

    private final OllamaService ollamaService;
    private final ProduitRepository produitRepository;
    private final CommandeProduitRepository commandeProduitRepository;
    private final ObjectMapper objectMapper;

    // Analyser tous les produits sous-performants
    @GetMapping("/analyser")
    public ResponseEntity<List<Map>> analyser() {
        LocalDateTime il_y_a_30_jours = LocalDateTime.now().minusDays(30);
        List<Produit> produits = produitRepository.findAll();
        List<Map> suggestions = new ArrayList<>();

        for (Produit produit : produits) {
            if (suggestions.size() >= 5) break; //  MAX 5

            int ventes = commandeProduitRepository
                    .countVentesDepuis(produit.getId(), il_y_a_30_jours);

            if (ventes < 2) {
                try {
                    String jsonResponse = ollamaService.analyserProduit(
                            produit.getNom(),
                            produit.getDescription(),
                            produit.getPrix(),
                            produit.getStock(),
                            ventes,
                            produit.getCategorie().getNom()
                    );
                    Map suggestion = objectMapper.readValue(jsonResponse, Map.class);
                    suggestion.put("produit_id", produit.getId());
                    suggestion.put("produit_nom", produit.getNom());
                    suggestion.put("stock_actuel", produit.getStock());
                    suggestion.put("ventes_recentes", ventes);
                    suggestions.add(suggestion);
                    System.out.println("✅ Analysé : " + produit.getNom());
                } catch (Exception e) {
                    System.err.println("❌ Erreur : " + produit.getNom()
                            + " → " + e.getMessage());
                }
            }
        }

        suggestions.sort((a, b) -> {
            int scoreA = (int) a.getOrDefault("score_urgence", 0);
            int scoreB = (int) b.getOrDefault("score_urgence", 0);
            return Integer.compare(scoreB, scoreA);
        });

        return ResponseEntity.ok(suggestions);
    }

    // Automatiser les solutions pour un produit
    @PostMapping("/automatiser/{produitId}")
    public ResponseEntity<Map> automatiser(
            @PathVariable Long produitId,
            @RequestBody Map<String, Object> body) {

        Produit produit = produitRepository
                .findById(produitId).orElseThrow();

        List<String> types = (List<String>)
                body.get("types");
        Map<String, Object> suggestion =
                (Map<String, Object>) body.get("suggestion");

        List<String> actionsEffectuees = new ArrayList<>();

        for (String type : types) {
            switch (type) {
                case "DESCRIPTION" -> {
                    produit.setDescription(
                            suggestion.get(
                                    "nouvelle_description").toString());
                    actionsEffectuees.add(
                            "Description mise à jour ✅");
                }
                case "PROMOTION" -> {
                    int pct = (int) suggestion
                            .get("promotion_recommandee");
                    double nouveauPrix = produit.getPrix()
                            * (1 - pct / 100.0);
                    produit.setPrix(
                            Math.round(nouveauPrix * 100.0) / 100.0);
                    actionsEffectuees.add(String.format(
                            "Prix réduit à %.2f€ (-%d%%) ✅",
                            nouveauPrix, pct));
                }
                case "TAGS" -> {
                    List<String> tags = (List<String>)
                            suggestion.get("tags_suggeres");
                    produit.setTags(String.join(",", tags));
                    actionsEffectuees.add(
                            "Tags SEO ajoutés ✅");
                }
            }
        }

        produitRepository.save(produit);

        return ResponseEntity.ok(Map.of(
                "produit_nom", produit.getNom(),
                "actions_effectuees", actionsEffectuees,
                "date_execution", LocalDateTime.now().toString()
        ));
    }

    @GetMapping("/analyser/{produitId}")
    public ResponseEntity<Map> analyserUnProduit(
            @PathVariable Long produitId) {

        Produit produit = produitRepository.findById(produitId)
                .orElseThrow();

        try {
            String jsonResponse = ollamaService.analyserProduit(
                    produit.getNom(),
                    produit.getDescription(),
                    produit.getPrix(),
                    produit.getStock(),
                    0,
                    produit.getCategorie().getNom()
            );

            Map suggestion = objectMapper.readValue(jsonResponse, Map.class);
            suggestion.put("produit_id", produit.getId());
            suggestion.put("produit_nom", produit.getNom());
            return ResponseEntity.ok(suggestion);

        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("error", e.getMessage()));
        }
    }
}