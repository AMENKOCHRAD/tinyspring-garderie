package com.tinyspring.garderie.repository.boutique;

import com.tinyspring.garderie.entity.boutique.Prediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PredictionRepository extends JpaRepository<Prediction, Long> {

    Optional<Prediction> findTopByProduitIdOrderByDateCalculDesc(Long produitId);

    List<Prediction> findByAlerteRuptureTrue();

    @Query("SELECT p FROM Prediction p ORDER BY p.alerteRupture DESC, p.totalPrevu4Semaines DESC")
    List<Prediction> findAllOrderByAlerteRuptureDescTotalPrevuDesc();
}