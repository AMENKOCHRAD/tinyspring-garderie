package com.tinyspring.garderie.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinyspring.garderie.entity.ObservationEnfant;
import com.tinyspring.garderie.service.ObservationDuplicateDetectionService.Result;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class ObservationDuplicateDetectionServiceImpl implements ObservationDuplicateDetectionService {

    private final ObjectMapper objectMapper;

    public ObservationDuplicateDetectionServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Result predictDuplicate(ObservationEnfant newObs, ObservationEnfant existingObs) {
        // Try ML notebook artifacts via python. If not available, fallback to a local similarity.
        Result ml = tryPredictWithPython(newObs, existingObs);
        if (ml != null) {
            return ml;
        }
        return fallbackHeuristic(newObs, existingObs);
    }

    private Result tryPredictWithPython(ObservationEnfant newObs, ObservationEnfant existingObs) {
        try {
            Path cwd = Path.of("").toAbsolutePath();
            // backend/garderie -> project root is parent of parent
            Path projectRoot = cwd.getParent() != null ? cwd.getParent().getParent() : null;
            if (projectRoot == null) {
                return null;
            }

            Path script = projectRoot.resolve("ml/duplicate_observation_detection/predict_pair_cli.py");
            Path artifacts = projectRoot.resolve("ml/duplicate_observation_detection/artifacts/dup_model.pkl");
            Path tfidf = projectRoot.resolve("ml/duplicate_observation_detection/artifacts/tfidf.pkl");
            if (!Files.exists(script) || !Files.exists(artifacts) || !Files.exists(tfidf)) {
                return null;
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("new", toCandidate(newObs));
            payload.put("existing", toCandidate(existingObs));

            String inputJson = objectMapper.writeValueAsString(payload);

            Process process = new ProcessBuilder("python", script.toString())
                    .redirectErrorStream(true)
                    .start();

            process.getOutputStream().write(inputJson.getBytes(StandardCharsets.UTF_8));
            process.getOutputStream().close();

            StringBuilder out = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    out.append(line);
                }
            }

            int code = process.waitFor();
            if (code != 0) {
                return null;
            }

            JsonNode node = objectMapper.readTree(out.toString());
            boolean dup = node.path("duplicate").asBoolean(false);
            double score = node.path("score").asDouble(0.0);
            return new Result(dup, score);
        } catch (Exception ignore) {
            return null;
        }
    }

    private Map<String, Object> toCandidate(ObservationEnfant obs) {
        Map<String, Object> c = new HashMap<>();
        c.put("type", obs.getType() != null ? obs.getType().name() : null);
        c.put("titre", obs.getTitre());
        c.put("description", obs.getDescription());
        c.put("creeLeIso", obs.getCreeLe() != null ? obs.getCreeLe().toString() : null);
        c.put("temperature", obs.getTemperature());
        return c;
    }

    private Result fallbackHeuristic(ObservationEnfant newObs, ObservationEnfant existingObs) {
        String a = normalize((newObs.getTitre() == null ? "" : newObs.getTitre()) + " " + (newObs.getDescription() == null ? "" : newObs.getDescription()));
        String b = normalize((existingObs.getTitre() == null ? "" : existingObs.getTitre()) + " " + (existingObs.getDescription() == null ? "" : existingObs.getDescription()));

        if (a.isBlank() || b.isBlank()) {
            return new Result(false, 0.0);
        }

        double jaccard = jaccardSimilarity(a, b);
        boolean sameType = newObs.getType() != null && newObs.getType().equals(existingObs.getType());

        // assez strict pour limiter les faux positifs
        boolean dup = sameType && jaccard >= 0.85;
        return new Result(dup, jaccard);
    }

    private String normalize(String s) {
        String x = s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
        x = x.replace('\n', ' ');
        x = x.replaceAll("[^\\p{L}\\p{N}\\s]+", " ");
        x = x.replaceAll("\\s+", " ").trim();
        return x;
    }

    private double jaccardSimilarity(String a, String b) {
        String[] aa = a.split(" ");
        String[] bb = b.split(" ");
        java.util.Set<String> sa = new java.util.HashSet<>();
        java.util.Set<String> sb = new java.util.HashSet<>();
        for (String t : aa) if (!t.isBlank()) sa.add(t);
        for (String t : bb) if (!t.isBlank()) sb.add(t);
        if (sa.isEmpty() || sb.isEmpty()) return 0.0;
        java.util.Set<String> inter = new java.util.HashSet<>(sa);
        inter.retainAll(sb);
        java.util.Set<String> uni = new java.util.HashSet<>(sa);
        uni.addAll(sb);
        return (double) inter.size() / (double) uni.size();
    }
}
