package com.tinyspring.garderie.service.boutique;

import com.tinyspring.garderie.dto.boutique.*;
import com.tinyspring.garderie.entity.boutique.*;
import com.tinyspring.garderie.entity.User;
import com.tinyspring.garderie.mapper.boutique.AffiniteMapper;
import com.tinyspring.garderie.repository.boutique.*;
import com.tinyspring.garderie.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AffiniteServiceImpl implements AffiniteService {

    private static final Map<String, Double> POIDS = Map.of(
            "VUE_3S",       2.0,
            "VUE_10S",      5.0,
            "VUE_30S",      10.0,
            "CLIC_DETAIL",  3.0,
            "RECHERCHE",    4.0,
            "AJOUT_PANIER", 15.0,
            "COMMANDE",     40.0,
            "ANNULATION",   -10.0
    );

    private final UserCategorieScoreRepository scoreRepo;
    private final UserInteractionRepository interactionRepo;
    private final ProduitRepository produitRepo;
    private final UserRepository userRepo;
    private final AffiniteMapper affiniteMapper;

    @Override
    @Transactional
    public void enregistrerInteraction(Long userId, InteractionRequestDto dto) {
        Double points = POIDS.getOrDefault(dto.getTypeInteraction(), 0.0);
        if (points == 0.0) return;
        Produit produit = produitRepo.findById(dto.getProduitId())
                .orElseThrow(() -> new RuntimeException("Produit introuvable"));
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User introuvable"));
        interactionRepo.save(UserInteraction.builder()
                .user(user).produit(produit)
                .typeInteraction(dto.getTypeInteraction())
                .points(points).build());
        Categorie categorie = produit.getCategorie();
        UserCategorieScore scoreEntity = scoreRepo
                .findByUserIdAndCategorieId(userId, categorie.getId())
                .orElseGet(() -> UserCategorieScore.builder()
                        .user(user).categorie(categorie)
                        .score(0.0).nbInteractions(0).build());
        scoreEntity.setScore(scoreEntity.getScore() * 0.98 + points);
        scoreEntity.setNbInteractions(scoreEntity.getNbInteractions() + 1);
        scoreEntity.setDerniereInteraction(java.time.LocalDateTime.now());
        scoreRepo.save(scoreEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProduitDto> getProduitRecommandes(Long userId) {
        List<UserCategorieScore> scores = scoreRepo.findByUserIdOrderByScoreDesc(userId);
        if (scores.isEmpty()) {
            return produitRepo.findByStockGreaterThan(0).stream()
                    .map(affiniteMapper::toProduitDto).collect(Collectors.toList());
        }
        Map<Long, Double> scoreMap = scores.stream().collect(Collectors.toMap(
                s -> s.getCategorie().getId(), UserCategorieScore::getScore));
        return produitRepo.findByStockGreaterThan(0).stream()
                .sorted(Comparator.comparingDouble(
                        (Produit p) -> scoreMap.getOrDefault(
                                p.getCategorie().getId(), 0.0)).reversed())
                .map(affiniteMapper::toProduitDto).collect(Collectors.toList());
    }

    @Override
    public List<UserCategorieScore> getScoresUser(Long userId) {
        return scoreRepo.findByUserIdOrderByScoreDesc(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserCategorieScoreAdminDto> getAllScoresAdmin() {
        return scoreRepo.findAll().stream()
                .map(affiniteMapper::toAdminDto)
                .sorted(Comparator.comparingDouble(
                        UserCategorieScoreAdminDto::getScore).reversed())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserCategorieScoreAdminDto> getScoresByUserAdmin(Long userId) {
        return scoreRepo.findByUserIdOrderByScoreDesc(userId).stream()
                .map(affiniteMapper::toAdminDto).collect(Collectors.toList());
    }
}