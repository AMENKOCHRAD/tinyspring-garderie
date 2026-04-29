package com.tinyspring.garderie.repository.boutique;

import com.tinyspring.garderie.entity.boutique.UserCategorieScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface UserCategorieScoreRepository extends JpaRepository<UserCategorieScore, Long> {

    Optional<UserCategorieScore> findByUserIdAndCategorieId(Long userId, Long categorieId);

    // Top catégories du user triées par score décroissant
    List<UserCategorieScore> findByUserIdOrderByScoreDesc(Long userId);

    // Pour le dashboard admin
    @Query("SELECT u FROM UserCategorieScore u WHERE u.user.id = :userId AND u.score > 0 ORDER BY u.score DESC")
    List<UserCategorieScore> findTopAffinitiesByUser(@Param("userId") Long userId);
}