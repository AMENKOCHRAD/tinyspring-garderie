package com.tinyspring.garderie.service;

import com.tinyspring.garderie.dto.RiskLevel;
import com.tinyspring.garderie.dto.RiskPredictionDto;
import com.tinyspring.garderie.entity.Enfant;
import com.tinyspring.garderie.entity.NiveauUrgence;
import com.tinyspring.garderie.entity.ObservationEnfant;
import com.tinyspring.garderie.entity.ObservationType;
import com.tinyspring.garderie.repository.EnfantRepository;
import com.tinyspring.garderie.repository.ObservationEnfantRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class RiskPredictionService {
// softmax model
    private static final int WINDOW_DAYS = 30;

    private final ObservationEnfantRepository observationRepository;
    private final EnfantRepository enfantRepository;

    private volatile SoftmaxModel model = null;
    private final Object modelLock = new Object();
    private volatile LocalDate modelTrainedForDay = null;

    public RiskPredictionService(ObservationEnfantRepository observationRepository,
                                 EnfantRepository enfantRepository) {
        this.observationRepository = observationRepository;
        this.enfantRepository = enfantRepository;
    }

    public RiskPredictionDto predirePourEnfant(Long enfantId) {
        Enfant enfant = enfantRepository.findById(enfantId)
                .orElseThrow(() -> new RuntimeException("Enfant introuvable"));

        ensureModelReady();

        List<ObservationEnfant> observations = observationRepository.findTop50ByEnfantIdOrderByCreeLeDesc(enfantId);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusDays(WINDOW_DAYS);

        List<ObservationEnfant> recent = (observations == null ? List.<ObservationEnfant>of() : observations)
                .stream()
                .filter(o -> o != null && o.getCreeLe() != null && !o.getCreeLe().isBefore(cutoff))
                .collect(Collectors.toList());

        Features features = computeFeatures(recent, now);
        double[] x = features.toVector(); //le resultat

        SoftmaxModel m = this.model; //Softmax (Régression Logistique Multiclasse) : un modèle de classification supervisée

//Il prédit une classe parmi plusieurs :
        double[] probs = m != null ? m.predictProba(x) : new double[]{0.55, 0.30, 0.15};

        int pred = argmax(probs);
        RiskLevel niveau = switch (pred) {
            case 2 -> RiskLevel.ELEVE;
            case 1 -> RiskLevel.MOYEN;
            default -> RiskLevel.FAIBLE;
        };

        RiskPredictionDto dto = new RiskPredictionDto();
        dto.setEnfantId(enfant.getId());
        dto.setEnfantNom(enfant.getNom());
        dto.setEnfantPrenom(enfant.getPrenom());
        dto.setFenetreJours(WINDOW_DAYS);
        dto.setObservationsUtilisees(recent.size());

        LocalDateTime last = recent.stream()
                .map(ObservationEnfant::getCreeLe)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
        dto.setDerniereObservationLe(last != null ? last.toString() : null);

        dto.setNiveau(niveau);
        dto.setConfiance(round4(max(probs)));
        dto.setFacteurs(buildFactors(features, recent));
        dto.setRecommandation(buildRecommendation(niveau));
        return dto;
    }

    private void ensureModelReady() {
        LocalDate today = LocalDate.now();
        if (model != null && Objects.equals(modelTrainedForDay, today)) {
            return;
        }

        synchronized (modelLock) {
            if (model != null && Objects.equals(modelTrainedForDay, today)) {
                return;
            }
            this.model = trainModelFromData();
            this.modelTrainedForDay = today;
        }
    }

    private SoftmaxModel trainModelFromData() { // le modele utilisé
        List<ObservationEnfant> all = observationRepository.findTop5000ByOrderByCreeLeDesc();
        List<Sample> samples = buildSamplesFromObservations(all);

        if (samples.size() < 200) {
            samples.addAll(buildSyntheticSamples(Math.max(0, 600 - samples.size()))); // //Données artificielles (si pas assez de data)
        }

        SoftmaxModel m = new SoftmaxModel(Features.FEATURE_COUNT, 3);
        m.fit(samples, 400, 0.05, 0.0005);
        return m;
    }

    private List<Sample> buildSamplesFromObservations(List<ObservationEnfant> observations) {
        if (observations == null || observations.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, List<ObservationEnfant>> byChild = observations.stream() //Regrouper les observations par enfant
                .filter(Objects::nonNull)
                .filter(o -> o.getEnfant() != null && o.getEnfant().getId() != null)
                .filter(o -> o.getCreeLe() != null)
                .collect(Collectors.groupingBy(o -> o.getEnfant().getId()));

        List<Sample> out = new ArrayList<>();

        for (Map.Entry<Long, List<ObservationEnfant>> entry : byChild.entrySet()) {
            List<ObservationEnfant> childObs = entry.getValue();
            childObs.sort(Comparator.comparing(ObservationEnfant::getCreeLe));

            List<LocalDate> referenceDays = childObs.stream()
                    .map(o -> o.getCreeLe().toLocalDate())
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());

            int step = Math.max(1, referenceDays.size() / 20);
            for (int i = 0; i < referenceDays.size(); i += step) {
                LocalDate refDay = referenceDays.get(i);
                LocalDateTime ref = refDay.atTime(23, 59, 59);
                LocalDateTime cutoff = ref.minusDays(WINDOW_DAYS);

                List<ObservationEnfant> window = childObs.stream()
                        .filter(o -> !o.getCreeLe().isAfter(ref))
                        .filter(o -> !o.getCreeLe().isBefore(cutoff))
                        .collect(Collectors.toList());

                if (window.isEmpty()) {
                    continue;
                }

                Features f = computeFeatures(window, ref);
                int y = pseudoLabel(f);
                out.add(new Sample(f.toVector(), y));
            }
        }

        return out;
    }

    private int pseudoLabel(Features f) {
        double score = 0.0;
        score += 0.45 * f.totalObs;
        score += 1.25 * f.incidentsCount;
        score += 0.90 * f.highUrgenceCount;
        score += 0.75 * f.tempHighCount;
        score += 0.60 * Math.max(0, f.trendUpCount);
        score += 0.25 * f.uniqueTypes;
        score += 0.20 * f.daysWithObs;

        if (score < 4.0) return 0;
        if (score < 8.0) return 1;
        return 2;
    }

    private List<Sample> buildSyntheticSamples(int count) {
        Random rnd = new Random(42);
        List<Sample> out = new ArrayList<>(Math.max(0, count));

        for (int i = 0; i < count; i++) {
            Features f = new Features();
            f.totalObs = rnd.nextInt(18);
            f.recent7 = rnd.nextInt(10);
            f.prev7 = rnd.nextInt(10);
            f.incidentsCount = rnd.nextInt(5);
            f.healthCount = rnd.nextInt(10);
            f.behaviorCount = rnd.nextInt(10);
            f.sleepCount = rnd.nextInt(8);
            f.foodCount = rnd.nextInt(8);
            f.highUrgenceCount = rnd.nextInt(6);
            f.avgUrgence = rnd.nextDouble() * 3.5;
            f.maxUrgence = rnd.nextInt(5);
            f.tempHighCount = rnd.nextInt(3);
            f.uniqueTypes = 1 + rnd.nextInt(7);
            f.daysWithObs = 1 + rnd.nextInt(10);
            f.trendUpCount = f.recent7 - f.prev7;

            if (f.incidentsCount >= 3) {
                f.highUrgenceCount += 2;
                f.avgUrgence = Math.min(4.0, f.avgUrgence + 1.0);
            }

            int y = pseudoLabel(f);
            out.add(new Sample(f.toVector(), y));
        }
        return out;
    }
//transformer en FEATURES
    private Features computeFeatures(List<ObservationEnfant> obs, LocalDateTime now) {
        Features f = new Features();
        if (obs == null || obs.isEmpty()) {
            return f;
        }

        LocalDateTime cutoff7 = now.minusDays(7);
        LocalDateTime cutoff14 = now.minusDays(14);

        int urgenceSum = 0;
        int urgenceCount = 0;
        int urgenceMax = 0;

        Set<ObservationType> types = new HashSet<>();
        Set<LocalDate> days = new HashSet<>();

        for (ObservationEnfant o : obs) {
            if (o == null || o.getCreeLe() == null) continue;

            f.totalObs++;
            types.add(o.getType());
            days.add(o.getCreeLe().toLocalDate());

            if (!o.getCreeLe().isBefore(cutoff7)) f.recent7++;
            else if (!o.getCreeLe().isBefore(cutoff14)) f.prev7++;

            if (o.getType() == ObservationType.INCIDENT) f.incidentsCount++;
            if (o.getType() == ObservationType.SANTE) f.healthCount++;
            if (o.getType() == ObservationType.COMPORTEMENT) f.behaviorCount++;
            if (o.getType() == ObservationType.SOMMEIL) f.sleepCount++;
            if (o.getType() == ObservationType.ALIMENTATION) f.foodCount++;

            int urg = urgenceScore(o.getUrgence());
            if (urg > 0) {
                urgenceSum += urg;
                urgenceCount++;
                urgenceMax = Math.max(urgenceMax, urg);
                if (urg >= 3) f.highUrgenceCount++;
            }

            if (o.getTemperature() != null && o.getTemperature() >= 38.0) {
                f.tempHighCount++;
            }
        }

        f.uniqueTypes = types.size();
        f.daysWithObs = days.size();
        f.avgUrgence = urgenceCount > 0 ? ((double) urgenceSum / urgenceCount) : 0.0;
        f.maxUrgence = urgenceMax;
        f.trendUpCount = f.recent7 - f.prev7;
        return f;
    }

    private int urgenceScore(NiveauUrgence urgence) {
        if (urgence == null) return 0;
        return switch (urgence) {
            case FAIBLE -> 1;
            case MOYENNE -> 2;
            case ELEVEE -> 3;
            case CRITIQUE -> 4;
        };
    }

    private List<String> buildFactors(Features f, List<ObservationEnfant> recent) {
        List<String> factors = new ArrayList<>();

        if (recent == null || recent.isEmpty()) {
            factors.add("Aucune observation recente dans les 30 derniers jours.");
            return factors;
        }

        if (f.totalObs >= 10) factors.add("Frequence elevee de changements (observations nombreuses).");
        if (f.trendUpCount >= 3) factors.add("Augmentation recente du nombre d'observations.");
        if (f.incidentsCount >= 2) factors.add("Incidents repetes sur la periode.");
        if (f.highUrgenceCount >= 2) factors.add("Plusieurs observations avec urgence elevee/critique.");
        if (f.tempHighCount >= 1) factors.add("Temperature elevee signalee.");
        if (f.sleepCount >= 5) factors.add("Changements de sommeil frequents.");
        if (f.behaviorCount >= 5) factors.add("Signaux comportementaux repetes.");

        if (factors.isEmpty()) {
            factors.add("Historique recent stable (pas de signal dominant).");
        }

        return factors;
    }

    private String buildRecommendation(RiskLevel level) {
        return switch (level) {
            case FAIBLE -> "Risque faible: continuer le suivi habituel. Noter les changements si un nouveau signal apparait.";
            case MOYEN -> "Risque moyen: surveiller de pres sur les prochains jours. Informer le parent si les signaux se repetent ou s'intensifient.";
            case ELEVE -> "Risque eleve: informer le parent rapidement et renforcer la surveillance. Si les signes persistent ou s'aggravent, suivre le protocole de la garderie et envisager un avis medical.";
        };
    }

    private static int argmax(double[] v) {
        int best = 0;
        double bestVal = -Double.MAX_VALUE;
        for (int i = 0; i < v.length; i++) {
            if (v[i] > bestVal) {
                bestVal = v[i];
                best = i;
            }
        }
        return best;
    }

    private static double max(double[] v) {
        double m = -Double.MAX_VALUE;
        for (double x : v) m = Math.max(m, x);
        return m;
    }

    private static double round4(double v) {
        return Math.round(v * 10_000.0) / 10_000.0;
    }

    private static class Features {
        static final int FEATURE_COUNT = 12;

        int totalObs = 0;
        int recent7 = 0;
        int prev7 = 0;
        int incidentsCount = 0;
        int healthCount = 0;
        int behaviorCount = 0;
        int sleepCount = 0;
        int foodCount = 0;
        int highUrgenceCount = 0;
        double avgUrgence = 0.0;
        int maxUrgence = 0;
        int tempHighCount = 0;
        int uniqueTypes = 0;
        int daysWithObs = 0;
        int trendUpCount = 0;

        double[] toVector() {
            return new double[] {
                    clamp01(totalObs / 18.0),
                    clamp01(recent7 / 10.0),
                    clamp01(prev7 / 10.0),
                    clamp01(incidentsCount / 5.0),
                    clamp01(healthCount / 10.0),
                    clamp01(behaviorCount / 10.0),
                    clamp01(sleepCount / 8.0),
                    clamp01(foodCount / 8.0),
                    clamp01(highUrgenceCount / 6.0),
                    clamp01(avgUrgence / 4.0),
                    clamp01(maxUrgence / 4.0),
                    clamp01(tempHighCount / 3.0)
            };
        }

        private static double clamp01(double v) {
            if (v < 0) return 0;
            if (v > 1) return 1;
            return v;
        }
    }

    private static class Sample {
        final double[] x;
        final int y;

        Sample(double[] x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    private static class SoftmaxModel {
        final int features;
        final int classes;
        final double[][] w; // [classes][features+1] bias

        SoftmaxModel(int features, int classes) {
            this.features = features;
            this.classes = classes;
            this.w = new double[classes][features + 1];
        }

        void fit(List<Sample> samples, int epochs, double lr, double l2) {
            if (samples == null || samples.isEmpty()) {
                return;
            }

            Random rnd = new Random(7);
            for (int c = 0; c < classes; c++) {
                for (int j = 0; j < features + 1; j++) {
                    w[c][j] = (rnd.nextDouble() - 0.5) * 0.02;
                }
            }

            List<Sample> shuffled = new ArrayList<>(samples);
            for (int epoch = 0; epoch < epochs; epoch++) {
                Collections.shuffle(shuffled, rnd);
                for (Sample s : shuffled) {
                    double[] proba = predictProba(s.x);
                    for (int c = 0; c < classes; c++) {
                        double y = (s.y == c) ? 1.0 : 0.0;
                        double err = (proba[c] - y);

                        w[c][0] -= lr * (err + l2 * w[c][0]);
                        for (int j = 0; j < features; j++) {
                            double grad = err * s.x[j] + l2 * w[c][j + 1];
                            w[c][j + 1] -= lr * grad;
                        }
                    }
                }
            }
        }

        double[] predictProba(double[] x) {
            double[] scores = new double[classes];
            for (int c = 0; c < classes; c++) {
                double s = w[c][0];
                for (int j = 0; j < features; j++) {
                    s += w[c][j + 1] * x[j];
                }
                scores[c] = s;
            }
            return softmax(scores);
        }

        private double[] softmax(double[] scores) {
            double max = -Double.MAX_VALUE;
            for (double s : scores) max = Math.max(max, s);

            double sum = 0;
            double[] exps = new double[scores.length];
            for (int i = 0; i < scores.length; i++) {
                double e = Math.exp(scores[i] - max);
                exps[i] = e;
                sum += e;
            }

            if (sum <= 0) {
                double[] uniform = new double[scores.length];
                Arrays.fill(uniform, 1.0 / scores.length);
                return uniform;
            }

            for (int i = 0; i < exps.length; i++) {
                exps[i] /= sum;
            }
            return exps;
        }
    }
}

