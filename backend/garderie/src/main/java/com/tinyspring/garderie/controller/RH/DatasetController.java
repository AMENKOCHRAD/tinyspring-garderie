package com.tinyspring.garderie.controller.RH;

import com.tinyspring.garderie.service.RH.IDatasetGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/dataset")
@RequiredArgsConstructor
public class DatasetController {

    private final IDatasetGeneratorService datasetGeneratorService;

    @PostMapping("/generer")
    public ResponseEntity<Map<String, String>> genererDataset() {
        try {
            String path = datasetGeneratorService.genererDataset();
            return ResponseEntity.ok(Map.of(
                    "message", "Dataset généré avec succès !",
                    "path", path
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}