package com.tinyspring.garderie.service.boutique;

import com.tinyspring.garderie.entity.boutique.Prediction;
import java.util.List;
import java.util.Map;

public interface PredictionService {
    void recalculerToutesLesPredictions();
    List<Prediction> getAllPredictions();
    List<Prediction> getAlertes();
    Map<String, Object> getPredictionLive(Long produitId);
    void recalculerMaintenant();
}