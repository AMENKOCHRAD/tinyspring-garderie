package com.tinyspring.garderie.controller.boutique;

import com.tinyspring.garderie.entity.boutique.Prediction;
import com.tinyspring.garderie.service.boutique.PredictionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/boutique/predictions")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:21065"})
public class PredictionController {

    private final PredictionService predictionService;

    public PredictionController(PredictionService predictionService) {
        this.predictionService = predictionService;
    }

    @GetMapping
    public ResponseEntity<List<Prediction>> getAll() {
        return ResponseEntity.ok(predictionService.getAllPredictions());
    }

    @GetMapping("/alertes")
    public ResponseEntity<List<Prediction>> getAlertes() {
        return ResponseEntity.ok(predictionService.getAlertes());
    }

    @GetMapping("/{produitId}/live")
    public ResponseEntity<Map<String, Object>> getLive(@PathVariable Long produitId) {
        return ResponseEntity.ok(predictionService.getPredictionLive(produitId));
    }

    @PostMapping("/recalculer")
    public ResponseEntity<String> recalculer() {
        predictionService.recalculerMaintenant();
        return ResponseEntity.ok("Recalcul lancé");
    }
}