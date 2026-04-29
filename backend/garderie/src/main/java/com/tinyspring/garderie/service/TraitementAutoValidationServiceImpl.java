package com.tinyspring.garderie.service;

import com.tinyspring.garderie.entity.*;
import com.tinyspring.garderie.repository.ConditionSanitaireRepository;
import com.tinyspring.garderie.repository.ObservationEnfantRepository;
import com.tinyspring.garderie.repository.TraitementRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinyspring.garderie.service.TraitementAutoValidationService.Decision;
import com.tinyspring.garderie.service.TraitementAutoValidationService.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Validation intelligente (hybride) :
 * 1) Règles métier (sécurité minimale) -> REFUSE / OK
 * 2) ML (Decision Tree) -> ACCEPTE / REFUSE / A_VERIFIER
 *
 * Sans microservices, modèle entraîné en mémoire à partir de l'historique (VALIDE/REFUSE).
 */
@Service
public class TraitementAutoValidationServiceImpl implements TraitementAutoValidationService {

    private static final int MAX_TRAINING_SAMPLES = 2000;
    private static final int MIN_TRAINING_SAMPLES = 30;
    private static final int MAX_DEPTH = 4;
    private static final int MIN_SAMPLES_SPLIT = 20;

    private final TraitementRepository traitementRepository;
    private final ConditionSanitaireRepository conditionSanitaireRepository;
    private final ObservationEnfantRepository observationEnfantRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.ml.traitement-model-path:models/traitement_validation_tree.json}")
    private String modelPath;

    private volatile DecisionTreeModel model = null;
    private final Object modelLock = new Object();
    private volatile LocalDate trainedForDay = null;
    private volatile boolean loadedFromFile = false;

    public TraitementAutoValidationServiceImpl(TraitementRepository traitementRepository,
                                           ConditionSanitaireRepository conditionSanitaireRepository,
                                           ObservationEnfantRepository observationEnfantRepository,
                                           ObjectMapper objectMapper) {
        this.traitementRepository = traitementRepository;
        this.conditionSanitaireRepository = conditionSanitaireRepository;
        this.observationEnfantRepository = observationEnfantRepository;
        this.objectMapper = objectMapper;
    }

    public Result evaluer(Traitement traitement) {
        Result result = new Result();

        List<String> ruleErrors = validateRules(traitement);
        if (!ruleErrors.isEmpty()) {
            result.decision = Decision.REFUSE;
            result.confiance = 1.0;
            result.facteurs = ruleErrors;
            result.note = "Refus automatique (règles): " + String.join(" | ", ruleErrors);
            result.source = "RULES";
            return result;
        }

        ensureModelReady();

        DecisionTreeModel m = this.model;
        if (m == null || m.sampleCount < MIN_TRAINING_SAMPLES) {
            result.decision = Decision.A_VERIFIER;
            result.confiance = 0.5;
            result.facteurs = buildFactors(traitement);
            result.note = "À vérifier: historique insuffisant pour une décision automatique fiable.";
            result.source = loadedFromFile ? "ML" : "FALLBACK";
            return result;
        }

        double[] x = computeFeatures(traitement);
        double pAccept = m.predictProbaAccept(x);

        result.facteurs = buildFactors(traitement);
        result.confiance = round3(Math.max(pAccept, 1.0 - pAccept));
        result.source = loadedFromFile ? "ML" : "FALLBACK";

        if (pAccept >= 0.75) {
            result.decision = Decision.ACCEPTE;
            result.note = "Acceptation automatique (ML). Recommandation: vérifier la cohérence générale et conserver l'ordonnance.";
            return result;
        }
        if (pAccept <= 0.25) {
            result.decision = Decision.REFUSE;
            result.note = "Refus automatique (ML). Recommandation: compléter/corriger les informations et réessayer.";
            return result;
        }

        result.decision = Decision.A_VERIFIER;
        result.note = "À vérifier: cas jugé à risque ou ambigu par le modèle (ML).";
        return result;
    }

    private List<String> validateRules(Traitement t) {
        List<String> errors = new ArrayList<>();
        if (t == null) {
            errors.add("Traitement manquant");
            return errors;
        }

        if (t.getConditionSanitaire() == null) {
            errors.add("Condition sanitaire manquante");
        }

        if (t.getOrdonnance() == null || t.getOrdonnance().isBlank()) {
            errors.add("Ordonnance absente");
        }

        if (t.getDateDebut() == null) {
            errors.add("Date de début manquante");
        } else if (t.getDateDebut().isBefore(LocalDate.now())) {
            errors.add("Date de début dans le passé");
        }

        if (t.getDateFin() != null && t.getDateDebut() != null && t.getDateFin().isBefore(t.getDateDebut())) {
            errors.add("Date de fin avant la date de début");
        }

        if (t.getDateFin() != null && t.getDateFin().isBefore(LocalDate.now())) {
            errors.add("Traitement déjà terminé");
        }

        if (t.getHeuresPrises() == null || t.getHeuresPrises().isEmpty()) {
            errors.add("Heures de prise manquantes");
        } else {
            List<String> heures = t.getHeuresPrises().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.toList());
            if (heures.isEmpty()) {
                errors.add("Heures de prise invalides");
            }
        }

        return errors;
    }

    private void ensureModelReady() {
        if (loadedFromFile && model != null) {
            return;
        }
        LocalDate today = LocalDate.now();
        if (model != null && Objects.equals(trainedForDay, today)) {
            return;
        }
        synchronized (modelLock) {
            if (model != null && Objects.equals(trainedForDay, today)) {
                return;
            }
            if (!loadedFromFile) {
                DecisionTreeModel loaded = tryLoadFromJson();
                if (loaded != null) {
                    this.model = loaded;
                    this.trainedForDay = today;
                    this.loadedFromFile = true;
                    return;
                }
            }

            this.model = trainFromHistory();
            this.trainedForDay = today;
        }
    }

    private DecisionTreeModel tryLoadFromJson() {
        try {
            String p = modelPath != null ? modelPath.trim() : "";
            if (p.isBlank()) {
                return null;
            }
            Path path = Path.of(p);
            if (!Files.exists(path)) {
                return null;
            }

            Snapshot snapshot = objectMapper.readValue(Files.readAllBytes(path), Snapshot.class);
            if (snapshot == null || snapshot.root == null) {
                return null;
            }
            Node root = snapshot.root.toNode();
            return new DecisionTreeModel(root, Math.max(0, snapshot.sampleCount));
        } catch (Exception ex) {
            return null;
        }
    }

    // JSON snapshot format produced by the notebook
    private static class Snapshot {
        public int sampleCount;
        public NodeSnapshot root;
    }

    private static class NodeSnapshot {
        public int featureIndex;
        public double threshold;
        public NodeSnapshot left;
        public NodeSnapshot right;
        public int count;
        public int positives;
        public boolean leaf;

        Node toNode() {
            Node n = new Node();
            n.featureIndex = featureIndex;
            n.threshold = threshold;
            n.count = count;
            n.positives = positives;
            n.leaf = leaf;
            n.left = left != null ? left.toNode() : null;
            n.right = right != null ? right.toNode() : null;
            return n;
        }
    }

    private DecisionTreeModel trainFromHistory() {
        List<Traitement> valides = safeList(traitementRepository.findByStatut(StatutTraitement.VALIDE));
        List<Traitement> refuses = safeList(traitementRepository.findByStatut(StatutTraitement.REFUSE));

        List<Sample> samples = new ArrayList<>();
        for (Traitement t : valides) {
            if (!isTrainingCandidate(t)) continue;
            samples.add(new Sample(computeFeatures(t), 1));
        }
        for (Traitement t : refuses) {
            if (!isTrainingCandidate(t)) continue;
            samples.add(new Sample(computeFeatures(t), 0));
        }

        if (samples.isEmpty()) {
            return new DecisionTreeModel(null, 0);
        }

        Collections.shuffle(samples, new Random(7));
        if (samples.size() > MAX_TRAINING_SAMPLES) {
            samples = samples.subList(0, MAX_TRAINING_SAMPLES);
        }

        Node root = buildTree(samples, 0);
        return new DecisionTreeModel(root, samples.size());
    }

    private boolean isTrainingCandidate(Traitement t) {
        if (t == null) return false;
        if (t.getConditionSanitaire() == null || t.getConditionSanitaire().getEnfant() == null) return false;
        if (t.getDateDebut() == null) return false;
        if (t.getHeuresPrises() == null || t.getHeuresPrises().isEmpty()) return false;
        if (t.getOrdonnance() == null || t.getOrdonnance().isBlank()) return false;
        return true;
    }

    // ========= Features =========
    // 0 ordonnance (0/1)
    // 1 durationDays (0..60)
    // 2 prisesPerDay (0..6)
    // 3 descLen (0..300)
    // 4 childAgeYears (0..12)
    // 5 hasChronic (0/1)
    // 6 recentIncidents (0..10)
    private double[] computeFeatures(Traitement t) {
        int ordonnance = (t.getOrdonnance() != null && !t.getOrdonnance().isBlank()) ? 1 : 0;
        long durationDays = 0;
        if (t.getDateDebut() != null) {
            LocalDate end = t.getDateFin() != null ? t.getDateFin() : t.getDateDebut().plusDays(7);
            durationDays = ChronoUnit.DAYS.between(t.getDateDebut(), end);
            if (durationDays < 0) durationDays = 0;
        }
        durationDays = Math.min(60, durationDays);

        int prisesPerDay = t.getHeuresPrises() != null ? (int) t.getHeuresPrises().stream().filter(h -> h != null && !h.trim().isBlank()).count() : 0;
        prisesPerDay = Math.min(6, Math.max(0, prisesPerDay));

        int descLen = (t.getDescription() != null ? t.getDescription().trim().length() : 0);
        descLen = Math.min(300, Math.max(0, descLen));

        Enfant enfant = t.getConditionSanitaire() != null ? t.getConditionSanitaire().getEnfant() : null;
        int ageYears = 0;
        if (enfant != null && enfant.getDateNaissance() != null) {
            ageYears = (int) ChronoUnit.YEARS.between(enfant.getDateNaissance(), LocalDate.now());
            ageYears = Math.min(12, Math.max(0, ageYears));
        }

        int hasChronic = 0;
        if (enfant != null && enfant.getId() != null) {
            List<ConditionSanitaire> conditions = safeList(conditionSanitaireRepository.findByEnfantId(enfant.getId()));
            hasChronic = conditions.stream().anyMatch(c -> c != null && c.getType() == TypeConditionSanitaire.MALADIE_CHRONIQUE) ? 1 : 0;
        }

        int recentIncidents = 0;
        if (enfant != null && enfant.getId() != null) {
            List<ObservationEnfant> obs = safeList(observationEnfantRepository.findTop50ByEnfantIdOrderByCreeLeDesc(enfant.getId()));
            LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
            recentIncidents = (int) obs.stream()
                    .filter(o -> o != null && o.getCreeLe() != null && !o.getCreeLe().isBefore(cutoff))
                    .filter(o -> o.getType() == ObservationType.INCIDENT || o.getType() == ObservationType.SANTE)
                    .count();
            recentIncidents = Math.min(10, Math.max(0, recentIncidents));
        }

        return new double[] {
                ordonnance,
                durationDays,
                prisesPerDay,
                descLen,
                ageYears,
                hasChronic,
                recentIncidents
        };
    }

    private List<String> buildFactors(Traitement t) {
        List<String> out = new ArrayList<>();
        if (t.getOrdonnance() != null && !t.getOrdonnance().isBlank()) out.add("Ordonnance fournie");
        if (t.getDateDebut() != null) out.add("Début: " + t.getDateDebut());
        if (t.getDateFin() != null) out.add("Fin: " + t.getDateFin());
        if (t.getHeuresPrises() != null && !t.getHeuresPrises().isEmpty()) out.add("Prises/jour: " + t.getHeuresPrises().size());
        if (t.getDescription() != null && !t.getDescription().trim().isBlank()) out.add("Description renseignée");

        Enfant enfant = t.getConditionSanitaire() != null ? t.getConditionSanitaire().getEnfant() : null;
        if (enfant != null && enfant.getDateNaissance() != null) {
            long age = ChronoUnit.YEARS.between(enfant.getDateNaissance(), LocalDate.now());
            out.add("Âge: " + age + " ans");
        }
        return out;
    }

    // ========= Decision Tree (CART-like, simplified) =========
    private static class Sample {
        final double[] x;
        final int y; // 1 accept, 0 refuse
        Sample(double[] x, int y) { this.x = x; this.y = y; }
    }

    private static class Node {
        int featureIndex = -1;
        double threshold = 0;
        Node left;
        Node right;
        int count = 0;
        int positives = 0;
        boolean leaf = true;
    }

    private static class DecisionTreeModel {
        final Node root;
        final int sampleCount;
        DecisionTreeModel(Node root, int sampleCount) {
            this.root = root;
            this.sampleCount = sampleCount;
        }
        double predictProbaAccept(double[] x) {
            if (root == null || x == null) return 0.5;
            Node n = root;
            while (n != null && !n.leaf) {
                double v = x[n.featureIndex];
                n = v <= n.threshold ? n.left : n.right;
            }
            if (n == null || n.count <= 0) return 0.5;
            return ((double) n.positives) / ((double) n.count);
        }
    }

    private Node buildTree(List<Sample> samples, int depth) {
        Node node = new Node();
        node.count = samples.size();
        node.positives = (int) samples.stream().filter(s -> s.y == 1).count();

        if (depth >= MAX_DEPTH || samples.size() < MIN_SAMPLES_SPLIT) {
            node.leaf = true;
            return node;
        }

        double baseImpurity = gini(node.positives, node.count);
        if (baseImpurity <= 0.0001) {
            node.leaf = true;
            return node;
        }

        int featureCount = samples.get(0).x.length;
        Split best = null;

        for (int fi = 0; fi < featureCount; fi++) {
            Split candidate = bestSplitForFeature(samples, fi);
            if (candidate != null && (best == null || candidate.gain > best.gain)) {
                best = candidate;
            }
        }

        if (best == null || best.gain <= 0.0001) {
            node.leaf = true;
            return node;
        }

        List<Sample> left = new ArrayList<>();
        List<Sample> right = new ArrayList<>();
        for (Sample s : samples) {
            if (s.x[best.featureIndex] <= best.threshold) left.add(s);
            else right.add(s);
        }

        if (left.isEmpty() || right.isEmpty()) {
            node.leaf = true;
            return node;
        }

        node.leaf = false;
        node.featureIndex = best.featureIndex;
        node.threshold = best.threshold;
        node.left = buildTree(left, depth + 1);
        node.right = buildTree(right, depth + 1);
        return node;
    }

    private static class Split {
        int featureIndex;
        double threshold;
        double gain;
    }

    private Split bestSplitForFeature(List<Sample> samples, int fi) {
        double[] values = new double[samples.size()];
        for (int i = 0; i < samples.size(); i++) values[i] = samples.get(i).x[fi];
        Arrays.sort(values);

        // take up to 10 quantile thresholds
        List<Double> thresholds = new ArrayList<>();
        int[] qs = new int[] {10, 20, 30, 40, 50, 60, 70, 80, 90};
        for (int q : qs) {
            int idx = (int) Math.floor((q / 100.0) * (values.length - 1));
            double thr = values[idx];
            thresholds.add(thr);
        }
        thresholds = thresholds.stream().distinct().collect(Collectors.toList());
        if (thresholds.isEmpty()) return null;

        int total = samples.size();
        int posTotal = (int) samples.stream().filter(s -> s.y == 1).count();
        double base = gini(posTotal, total);

        Split best = null;
        for (double thr : thresholds) {
            int leftCount = 0, leftPos = 0, rightCount = 0, rightPos = 0;
            for (Sample s : samples) {
                if (s.x[fi] <= thr) {
                    leftCount++;
                    if (s.y == 1) leftPos++;
                } else {
                    rightCount++;
                    if (s.y == 1) rightPos++;
                }
            }
            if (leftCount == 0 || rightCount == 0) continue;
            double impurity = ((double) leftCount / total) * gini(leftPos, leftCount)
                    + ((double) rightCount / total) * gini(rightPos, rightCount);
            double gain = base - impurity;
            if (best == null || gain > best.gain) {
                Split s = new Split();
                s.featureIndex = fi;
                s.threshold = thr;
                s.gain = gain;
                best = s;
            }
        }
        return best;
    }

    private static double gini(int positives, int total) {
        if (total <= 0) return 0;
        double p = (double) positives / (double) total;
        double n = 1.0 - p;
        return 1.0 - (p * p + n * n);
    }

    private static <T> List<T> safeList(List<T> in) {
        return in != null ? in : List.of();
    }

    private static double round3(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}
