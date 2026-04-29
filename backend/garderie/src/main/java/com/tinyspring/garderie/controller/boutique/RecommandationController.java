package com.tinyspring.garderie.controller.boutique;

import com.tinyspring.garderie.dto.boutique.InteractionRequestDto;
import com.tinyspring.garderie.dto.boutique.ProduitDto;
import com.tinyspring.garderie.entity.User;
import com.tinyspring.garderie.service.boutique.AffiniteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import com.tinyspring.garderie.repository.UserRepository;

import java.util.List;

@RestController
@RequestMapping("/api/boutique")
public class RecommandationController {

    private final AffiniteService affiniteService;
    private final UserRepository userRepository;

    public RecommandationController(AffiniteService affiniteService,
                                    UserRepository userRepository) {
        this.affiniteService = affiniteService;
        this.userRepository = userRepository;
    }

    // ── GET produits recommandés pour le user connecté ────────────────────────
    @GetMapping("/produits/recommandes")
    public ResponseEntity<List<ProduitDto>> getRecommandes(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User introuvable"));

        return ResponseEntity.ok(affiniteService.getProduitRecommandes(user.getId()));
    }

    // ── POST enregistrer une interaction ─────────────────────────────────────
    @PostMapping("/interactions")
    public ResponseEntity<Void> interaction(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody InteractionRequestDto dto) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User introuvable"));

        affiniteService.enregistrerInteraction(user.getId(), dto);
        return ResponseEntity.ok().build();
    }
}