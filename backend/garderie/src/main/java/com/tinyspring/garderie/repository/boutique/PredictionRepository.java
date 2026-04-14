package com.tinyspring.garderie.repository.boutique;

import com.tinyspring.garderie.entity.boutique.Prediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PredictionRepository extends JpaRepository<Prediction, Long> {

    Optional<Prediction> findTopByProduitIdOrderByDateCalculDesc(Long produitId);

    List<Prediction> findByAlerteRuptureTrue();

    List<Prediction> findAllByOrderByAlerteRuptureTotalPrevu4SemainesDesc();
}