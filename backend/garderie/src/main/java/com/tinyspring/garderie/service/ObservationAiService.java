package com.tinyspring.garderie.service;

import com.tinyspring.garderie.dto.ObservationAiRequestDto;
import com.tinyspring.garderie.dto.ObservationAiResponseDto;
import com.tinyspring.garderie.entity.Enfant;
import com.tinyspring.garderie.entity.NiveauUrgence;
import com.tinyspring.garderie.entity.ObservationEnfant;
import com.tinyspring.garderie.entity.ObservationType;
import com.tinyspring.garderie.repository.EnfantRepository;
import com.tinyspring.garderie.repository.ObservationEnfantRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

@Service
public class ObservationAiService {

    private final EnfantRepository enfantRepository;
    private final ObservationEnfantRepository observationEnfantRepository;
    private final OpenAiResponsesClient openAiClient;

    public ObservationAiService(EnfantRepository enfantRepository,
                                ObservationEnfantRepository observationEnfantRepository,
                                OpenAiResponsesClient openAiClient) {
        this.enfantRepository = enfantRepository;
        this.observationEnfantRepository = observationEnfantRepository;
        this.openAiClient = openAiClient;
    }

    /**
     * "IA" locale (sans API externe): genere une description structuree et adaptee au type choisi.
     * Objectif: aider l'animatrice a rediger vite, de facon claire et actionnable.
     */
    public ObservationAiResponseDto generer(ObservationAiRequestDto req) {
        if (req == null) {
            throw new RuntimeException("Requete invalide.");
        }

        if (req.getEnfantId() == null) {
            throw new RuntimeException("Enfant obligatoire.");
        }

        Enfant enfant = enfantRepository.findById(req.getEnfantId())
                .orElseThrow(() -> new RuntimeException("Enfant introuvable."));

        ObservationType type = req.getType();
        if (type == null) {
            throw new RuntimeException("Type obligatoire.");
        }

        String titre = safe(req.getTitre());
        if (titre.isBlank()) {
            throw new RuntimeException("Titre obligatoire.");
        }

        String enfantNom = safe(enfant.getPrenom()) + " " + safe(enfant.getNom());
        NiveauUrgence urgence = req.getUrgence() != null ? req.getUrgence() : NiveauUrgence.MOYENNE;
        Double temperature = req.getTemperature();

        String symptomes = safe(req.getSymptomes());
        String actions = safe(req.getActionsEffectuees());
        String contexte = safe(req.getContexte());

        List<String> contexteRecent = buildContexteRecent(req.getEnfantId());

        if (openAiClient != null && openAiClient.isEnabled()) {
            return genererAvecOpenAi(req, enfant, contexteRecent);
        }

        String resume = "[" + labelType(type) + "] " + titre + " (" + urgence.name() + ")";

        StringBuilder desc = new StringBuilder();
        desc.append("Objet: ").append(titre).append('\n');
        desc.append('\n');
        desc.append("Synthese:\n");
        desc.append("- ").append(enfantNom).append('\n');
        desc.append("- Type: ").append(labelType(type)).append('\n');
        desc.append("- Urgence: ").append(labelUrgence(urgence)).append('\n');
        if (temperature != null) {
            desc.append("- Temperature: ").append(temperature).append(" °C\n");
        }
        if (!safe(req.getLieu()).isBlank()) {
            desc.append("- Lieu: ").append(safe(req.getLieu())).append('\n');
        }
        if (!safe(enfant.getAllergies()).isBlank()) {
            desc.append("- Allergies connues: ").append(safe(enfant.getAllergies())).append('\n');
        }
        if (!contexteRecent.isEmpty()) {
            desc.append("- Contexte recent: ").append(contexteRecent.size()).append(" observation(s) recente(s)\n");
        }
        desc.append('\n');

        // Section adaptee au type
        desc.append("Details:\n");
        switch (type) {
            case SANTE -> {
                writeParagraph(desc, "Symptomes observes", symptomes);
                writeParagraph(desc, "Contexte (repas/sommeil/activite)", contexte);
                writeParagraph(desc, "Actions deja faites", actions);
                desc.append("- Recommandation: surveiller l'evolution et informer le parent.\n");
            }
            case COMPORTEMENT -> {
                writeParagraph(desc, "Comportement observe", symptomes);
                writeParagraph(desc, "Declencheur / contexte", contexte);
                writeParagraph(desc, "Actions educatives mises en place", actions);
                desc.append("- Recommandation: echanger avec le parent pour aligner les habitudes.\n");
            }
            case SOMMEIL -> {
                writeParagraph(desc, "Etat du sommeil", symptomes);
                writeParagraph(desc, "Contexte (sieste/nuit preced.)", contexte);
                writeParagraph(desc, "Actions", actions);
                desc.append("- Recommandation: suivre la prochaine sieste et noter les horaires.\n");
            }
            case ALIMENTATION -> {
                writeParagraph(desc, "Observation alimentation", symptomes);
                writeParagraph(desc, "Contexte (menu/quantite)", contexte);
                writeParagraph(desc, "Actions", actions);
                desc.append("- Recommandation: confirmer preferences/allergies avec le parent.\n");
            }
            case INCIDENT -> {
                writeParagraph(desc, "Incident", symptomes);
                writeParagraph(desc, "Contexte", contexte);
                writeParagraph(desc, "Actions + soins", actions);
                desc.append("- Recommandation: informer le parent et preciser les soins effectues.\n");
            }
            case HUMEUR -> {
                writeParagraph(desc, "Humeur", symptomes);
                writeParagraph(desc, "Contexte", contexte);
                writeParagraph(desc, "Actions", actions);
                desc.append("- Recommandation: surveiller la suite de la journee.\n");
            }
            case HYGIENE -> {
                writeParagraph(desc, "Hygiene", symptomes);
                writeParagraph(desc, "Contexte", contexte);
                writeParagraph(desc, "Actions", actions);
                desc.append("- Recommandation: partager les consignes avec le parent si besoin.\n");
            }
            default -> {
                writeParagraph(desc, "Observation", symptomes);
                writeParagraph(desc, "Contexte", contexte);
                writeParagraph(desc, "Actions", actions);
            }
        }

        // Ensure no empty placeholders
        String description = desc.toString().trim();

        // Advanced: propose message + checklists
        List<String> actionsProposees = buildActionsProposees(type, urgence, temperature, actions);
        List<String> pointsASurveiller = buildPointsASurveiller(type, urgence, temperature);
        List<String> questions = buildQuestions(type);
        String messageParent = buildMessageParent(enfantNom, type, titre, urgence, temperature, safe(req.getLieu()), symptomes);
        List<String> problemesPossibles = buildProblemesPossibles(type, urgence, temperature, symptomes, contexte, enfant.getAllergies(), contexteRecent);

        AdvancedSignals advanced = buildAdvancedSignals(type, urgence, temperature, titre, symptomes, contexte, contexteRecent);
        List<String> similaires = buildObservationsSimilaires(req.getEnfantId(), type, titre, symptomes);

        return new ObservationAiResponseDto(
                description,
                resume,
                messageParent,
                actionsProposees,
                pointsASurveiller,
                questions,
                problemesPossibles,
                contexteRecent,
                advanced.urgenceSuggeree,
                advanced.scoreUrgence,
                advanced.tags,
                advanced.signauxDetectes,
                similaires
        );
    }

    private ObservationAiResponseDto genererAvecOpenAi(ObservationAiRequestDto req, Enfant enfant, List<String> contexteRecent) {
        ObservationType type = req.getType();
        String titre = safe(req.getTitre());
        NiveauUrgence urgence = req.getUrgence() != null ? req.getUrgence() : NiveauUrgence.MOYENNE;
        Double temperature = req.getTemperature();

        String enfantNom = safe(enfant.getPrenom()) + " " + safe(enfant.getNom());
        String lieu = safe(req.getLieu());
        String symptomes = safe(req.getSymptomes());
        String actions = safe(req.getActionsEffectuees());
        String contexte = safe(req.getContexte());
        String allergies = safe(enfant.getAllergies());

        List<String> similaires = buildObservationsSimilaires(req.getEnfantId(), type, titre, symptomes);

        String system = """
                Tu es un assistant pour une garderie (TinySpring). Tu aides une animatrice à rédiger une observation pour les parents.
                Règles :
                - Ne jamais poser de diagnostic médical.
                - Utiliser un langage prudent : "possible", "probable", "à surveiller", "sans conclusion médicale".
                - Rester factuel, clair, et actionnable.
                - Écrire en français.
                - Si une information manque, répondre avec des listes vides / valeur prudente (ne pas inventer).
                """.trim();

        StringBuilder user = new StringBuilder();
        user.append("Contexte enfant:\n");
        user.append("- Enfant: ").append(enfantNom).append("\n");
        if (!allergies.isBlank()) {
            user.append("- Allergies connues: ").append(allergies).append("\n");
        }
        if (contexteRecent != null && !contexteRecent.isEmpty()) {
            user.append("- Contexte récent (titres): ").append(contexteRecent).append("\n");
        }
        if (similaires != null && !similaires.isEmpty()) {
            user.append("- Observations similaires (historique): ").append(similaires).append("\n");
        }

        user.append("\nObservation du jour (entrée animatrice):\n");
        user.append("- Type: ").append(type != null ? type.name() : "").append("\n");
        user.append("- Titre: ").append(titre).append("\n");
        user.append("- Urgence saisie: ").append(urgence.name()).append("\n");
        if (temperature != null) {
            user.append("- Température: ").append(temperature).append("°C\n");
        }
        if (!lieu.isBlank()) {
            user.append("- Lieu: ").append(lieu).append("\n");
        }
        if (!symptomes.isBlank()) {
            user.append("- Ce qui a été observé: ").append(symptomes).append("\n");
        }
        if (!contexte.isBlank()) {
            user.append("- Contexte: ").append(contexte).append("\n");
        }
        if (!actions.isBlank()) {
            user.append("- Actions effectuées: ").append(actions).append("\n");
        }

        user.append("""

                Tâche:
                Génère:
                - description structurée (pour archive)
                - messageParent court (SMS/notification)
                - problèmes possibles (hypothèses prudentes)
                - actions proposées, points à surveiller, questions aux parents
                - urgence suggérée + score (0..100) basé sur les signaux de l'observation et la récence/historique
                - tags et signaux détectés (courts)

                Important:
                - Ne pas inventer des maladies.
                - Si tu mentionnes un risque, ajoute "possible" / "à surveiller".
                """);

        Map<String, Object> schema = buildOpenAiSchema();
        return openAiClient.createJsonSchemaResponse(system, user.toString(), schema, ObservationAiResponseDto.class);
    }

    private Map<String, Object> buildOpenAiSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);

        Map<String, Object> props = new HashMap<>();
        props.put("description", Map.of("type", "string"));
        props.put("resume", Map.of("type", "string"));
        props.put("messageParent", Map.of("type", "string"));
        props.put("actionsProposees", Map.of("type", "array", "items", Map.of("type", "string")));
        props.put("pointsASurveiller", Map.of("type", "array", "items", Map.of("type", "string")));
        props.put("questionsAuxParents", Map.of("type", "array", "items", Map.of("type", "string")));
        props.put("problemesPossibles", Map.of("type", "array", "items", Map.of("type", "string")));
        props.put("contexteRecent", Map.of("type", "array", "items", Map.of("type", "string")));
        props.put("urgenceSuggeree", Map.of("type", "string", "enum", List.of("FAIBLE", "MOYENNE", "ELEVEE", "CRITIQUE")));
        props.put("scoreUrgence", Map.of("type", "integer", "minimum", 0, "maximum", 100));
        props.put("tags", Map.of("type", "array", "items", Map.of("type", "string")));
        props.put("signauxDetectes", Map.of("type", "array", "items", Map.of("type", "string")));
        props.put("observationsSimilaires", Map.of("type", "array", "items", Map.of("type", "string")));

        schema.put("properties", props);
        schema.put("required", List.of(
                "description",
                "resume",
                "messageParent",
                "actionsProposees",
                "pointsASurveiller",
                "questionsAuxParents",
                "problemesPossibles",
                "contexteRecent",
                "urgenceSuggeree",
                "scoreUrgence",
                "tags",
                "signauxDetectes",
                "observationsSimilaires"
        ));
        return schema;
    }

    private void writeParagraph(StringBuilder sb, String title, String content) {
        if (content == null || content.isBlank()) {
            return;
        }
        sb.append("- ").append(title).append(": ").append(content.trim()).append('\n');
    }

    private String labelType(ObservationType type) {
        return switch (type) {
            case SANTE -> "Sante";
            case COMPORTEMENT -> "Comportement";
            case SOMMEIL -> "Sommeil";
            case ALIMENTATION -> "Alimentation";
            case INCIDENT -> "Incident";
            case HUMEUR -> "Humeur";
            case HYGIENE -> "Hygiene";
            case JOUET -> "Jouet";
            case ACTIVITE -> "Activite";
            case AUTRE -> "Autre";
        };
    }

    private String labelUrgence(NiveauUrgence urgence) {
        if (urgence == null) {
            return "Moyenne";
        }
        return switch (urgence) {
            case FAIBLE -> "Faible";
            case MOYENNE -> "Moyenne";
            case ELEVEE -> "Elevee";
            case CRITIQUE -> "Critique";
        };
    }

    private void addIfNotBlank(List<String> list, String value) {
        if (value != null && !value.isBlank()) {
            list.add(value);
        }
    }

    private String safe(String value) {
        return value != null ? value.trim() : "";
    }

    private static final class AdvancedSignals {
        private final String urgenceSuggeree;
        private final Integer scoreUrgence;
        private final List<String> tags;
        private final List<String> signauxDetectes;

        private AdvancedSignals(String urgenceSuggeree, Integer scoreUrgence, List<String> tags, List<String> signauxDetectes) {
            this.urgenceSuggeree = urgenceSuggeree;
            this.scoreUrgence = scoreUrgence;
            this.tags = tags;
            this.signauxDetectes = signauxDetectes;
        }
    }

    private AdvancedSignals buildAdvancedSignals(ObservationType type,
                                                NiveauUrgence urgenceSaisie,
                                                Double temperature,
                                                String titre,
                                                String symptomes,
                                                String contexte,
                                                List<String> contexteRecent) {
        String txt = (safe(titre) + " " + safe(symptomes) + " " + safe(contexte)).toLowerCase(Locale.ROOT);
        List<String> signaux = new ArrayList<>();
        Set<String> tags = new HashSet<>();

        tags.add("TYPE_" + (type != null ? type.name() : "AUTRE"));

        int score = 20;

        if (temperature != null) {
            if (temperature >= 39.0) {
                score += 35;
                signaux.add("Temperature >= 39°C");
                tags.add("FIEVRE_FORTE");
            } else if (temperature >= 38.0) {
                score += 20;
                signaux.add("Temperature >= 38°C");
                tags.add("FIEVRE");
            } else if (temperature >= 37.5) {
                score += 10;
                tags.add("TEMPERATURE_ELEVEE");
            }
        }

        // Simple "red flags" lexicon (non medical, indicatif)
        if (containsAny(txt, "difficulte a respirer", "respire mal", "etouffe", "siffle")) {
            score += 40;
            signaux.add("Gene respiratoire mentionnee");
            tags.add("RESPIRATION");
        }
        if (containsAny(txt, "saigne", "sang", "hemorrag", "plaie")) {
            score += 25;
            signaux.add("Saignement/plaie mentionne");
            tags.add("BLESSURE");
        }
        if (containsAny(txt, "vomit", "vomissement", "diarrh", "dehydrate")) {
            score += 20;
            signaux.add("Troubles digestifs mentionnes");
            tags.add("DIGESTIF");
        }
        if (containsAny(txt, "allerg", "urticaire", "gonflement", "oedeme")) {
            score += 25;
            signaux.add("Signal allergique possible");
            tags.add("ALLERGIE");
        }
        if (containsAny(txt, "refuse", "refus", "ne mange pas", "pas mange", "appetit")) {
            score += 10;
            tags.add("ALIMENTATION");
        }
        if (containsAny(txt, "dort", "sieste", "insom", "reveille")) {
            score += 8;
            tags.add("SOMMEIL");
        }
        if (containsAny(txt, "colere", "agite", "agitation", "pleure", "cris", "mord", "tape")) {
            score += 10;
            tags.add("COMPORTEMENT");
        }
        if (containsAny(txt, "jouet", "partage", "dispute", "bouscul")) {
            score += 6;
            tags.add("JOUET");
        }

        // Trend signal: repeated type in recent context
        if (contexteRecent != null && !contexteRecent.isEmpty() && type != null) {
            long sameType = contexteRecent.stream().filter(s -> s != null && s.contains(type.name())).count();
            if (sameType >= 2) {
                score += 10;
                signaux.add("Repetition recente (" + sameType + " fois) sur le meme type");
                tags.add("RECURRENT");
            }
        }

        // Clamp
        if (score < 0) score = 0;
        if (score > 100) score = 100;

        String urgenceSuggeree;
        if (score >= 80) {
            urgenceSuggeree = "CRITIQUE";
        } else if (score >= 60) {
            urgenceSuggeree = "ELEVEE";
        } else if (score >= 35) {
            urgenceSuggeree = "MOYENNE";
        } else {
            urgenceSuggeree = "FAIBLE";
        }

        // If user already picked higher urgency, keep it (helpful UX)
        if (urgenceSaisie != null) {
            if (urgenceSaisie == NiveauUrgence.CRITIQUE) {
                urgenceSuggeree = "CRITIQUE";
            } else if (urgenceSaisie == NiveauUrgence.ELEVEE && !"CRITIQUE".equals(urgenceSuggeree)) {
                urgenceSuggeree = "ELEVEE";
            }
        }

        List<String> tagList = tags.stream().sorted().toList();
        return new AdvancedSignals(urgenceSuggeree, score, tagList, signaux);
    }

    private boolean containsAny(String txt, String... needles) {
        if (txt == null || txt.isBlank() || needles == null) {
            return false;
        }
        for (String n : needles) {
            if (n != null && !n.isBlank() && txt.contains(n)) {
                return true;
            }
        }
        return false;
    }

    private List<String> buildObservationsSimilaires(Long enfantId, ObservationType type, String titre, String symptomes) {
        if (enfantId == null) {
            return List.of();
        }

        String query = (safe(titre) + " " + safe(symptomes)).toLowerCase(Locale.ROOT);
        if (query.isBlank()) {
            return List.of();
        }

        Set<String> keywords = extractKeywords(query);
        if (keywords.isEmpty()) {
            return List.of();
        }

        List<ObservationEnfant> recents = observationEnfantRepository.findTop50ByEnfantIdOrderByCreeLeDesc(enfantId);
        if (recents == null || recents.isEmpty()) {
            return List.of();
        }

        List<String> out = new ArrayList<>();
        for (ObservationEnfant o : recents) {
            if (o == null || o.getTitre() == null) {
                continue;
            }
            if (type != null && o.getType() != null && o.getType() != type) {
                // Prefer same type (but still allow matches)
            }
            String candidate = (safe(o.getTitre()) + " " + safe(o.getDescription())).toLowerCase(Locale.ROOT);
            int overlap = overlapCount(keywords, extractKeywords(candidate));
            if (overlap >= 2) {
                String t = o.getType() != null ? o.getType().name() : "";
                String when = o.getCreeLe() != null ? o.getCreeLe().toLocalDate().toString() : "";
                out.add(when + " - " + t + " - " + safe(o.getTitre()));
            }
            if (out.size() >= 5) {
                break;
            }
        }
        return out;
    }

    private Set<String> extractKeywords(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }
        String cleaned = text
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-zàâçéèêëîïôûùüÿñæœ0-9\\s-]", " ")
                .replace('-', ' ');
        String[] parts = cleaned.split("\\s+");
        Set<String> out = new HashSet<>();
        for (String p : parts) {
            if (p == null) continue;
            String w = p.trim();
            if (w.length() < 4) continue;
            if (isStopWord(w)) continue;
            out.add(w);
            if (out.size() >= 24) break;
        }
        return out;
    }

    private boolean isStopWord(String w) {
        return switch (w) {
            case "avec", "pour", "dans", "chez", "plus", "moins", "tres", "trop",
                 "comme", "mais", "donc", "alors", "quand", "avant", "apres",
                 "enfant", "petit", "petite", "aujourd", "aujourdhui", "hier",
                 "nous", "vous", "bonjour", "merci", "objet", "type", "urgence",
                 "synthese", "details", "contexte", "actions", "observe" -> true;
            default -> false;
        };
    }

    private int overlapCount(Set<String> a, Set<String> b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) {
            return 0;
        }
        int c = 0;
        for (String x : a) {
            if (b.contains(x)) {
                c++;
            }
        }
        return c;
    }

    private List<String> buildActionsProposees(ObservationType type, NiveauUrgence urgence, Double temperature, String actionsDejaFaites) {
        List<String> out = new ArrayList<>();
        if (actionsDejaFaites == null || actionsDejaFaites.isBlank()) {
            out.add("Noter les actions deja effectuees (soins, surveillance, appel).");
        }

        if (urgence == NiveauUrgence.CRITIQUE || urgence == NiveauUrgence.ELEVEE) {
            out.add("Informer le parent immediatement (appel + message).");
        } else {
            out.add("Informer le parent via la section Changements.");
        }

        if (type == ObservationType.SANTE && temperature != null && temperature >= 38.5) {
            out.add("Surveiller la temperature et proposer hydratation.");
        }

        if (type == ObservationType.INCIDENT) {
            out.add("Verifier l'etat general + noter l'evolution (douleur, hematome).");
        }

        return out;
    }

    private List<String> buildPointsASurveiller(ObservationType type, NiveauUrgence urgence, Double temperature) {
        List<String> out = new ArrayList<>();
        if (type == ObservationType.SANTE) {
            out.add("Evolution des symptomes (aggravation / amelioration).");
            if (temperature != null && temperature >= 38.0) {
                out.add("Temperature toutes les 2-3 heures si possible.");
            }
            out.add("Hydratation + appetit.");
        } else if (type == ObservationType.COMPORTEMENT) {
            out.add("Frequence du comportement + declencheurs.");
            out.add("Interaction avec les autres enfants.");
        } else if (type == ObservationType.SOMMEIL) {
            out.add("Horaire de sieste et qualite du sommeil.");
        } else if (type == ObservationType.ALIMENTATION) {
            out.add("Quantite mangee + intolerances.");
        } else if (type == ObservationType.INCIDENT) {
            out.add("Douleur, gonflement, rougeur.");
            out.add("Marche / mobilisation selon la zone.");
        } else if (type == ObservationType.HUMEUR) {
            out.add("Humeur (tristesse/irritabilite) et duree.");
        } else if (type == ObservationType.HYGIENE) {
            out.add("Irritations, rougeurs, confort de l'enfant.");
        }

        if (urgence == NiveauUrgence.CRITIQUE) {
            out.add("Si signes graves: contacter services d'urgence selon le protocole.");
        }
        return out;
    }

    private List<String> buildQuestions(ObservationType type) {
        List<String> out = new ArrayList<>();
        switch (type) {
            case SANTE -> {
                out.add("L'enfant a-t-il eu des symptomes similaires recemment ?");
                out.add("Y a-t-il un traitement en cours (sirop, spray, etc.) ?");
            }
            case COMPORTEMENT -> {
                out.add("Y a-t-il eu un changement a la maison (sommeil, routine) ?");
                out.add("Avez-vous des strategies qui fonctionnent habituellement ?");
            }
            case SOMMEIL -> out.add("Comment s'est passee la nuit precedente ?");
            case ALIMENTATION -> out.add("Y a-t-il des preferences ou restrictions recentes ?");
            case INCIDENT -> out.add("Souhaitez-vous un appel pour plus de details ?");
            default -> out.add("Avez-vous des informations utiles a partager ?");
        }
        return out;
    }

    private String buildMessageParent(String enfantNom,
                                     ObservationType type,
                                     String titre,
                                     NiveauUrgence urgence,
                                     Double temperature,
                                     String lieu,
                                     String symptomes) {
        StringBuilder sb = new StringBuilder();
        sb.append("Bonjour, ");
        sb.append("nous vous informons d'un changement concernant ").append(enfantNom).append(". ");
        sb.append("Sujet: ").append(titre).append(" (").append(labelType(type)).append("). ");
        if (temperature != null) {
            sb.append("Temperature: ").append(temperature).append(" °C. ");
        }
        if (lieu != null && !lieu.isBlank()) {
            sb.append("Lieu: ").append(lieu.trim()).append(". ");
        }
        if (symptomes != null && !symptomes.isBlank()) {
            sb.append("Observation: ").append(symptomes.trim()).append(". ");
        }
        if (urgence == NiveauUrgence.ELEVEE || urgence == NiveauUrgence.CRITIQUE) {
            sb.append("Merci de nous contacter des que possible.");
        } else {
            sb.append("Nous restons disponibles si besoin.");
        }
        return sb.toString().trim();
    }

    private List<String> buildContexteRecent(Long enfantId) {
        if (enfantId == null) {
            return List.of();
        }
        List<ObservationEnfant> recents = observationEnfantRepository.findTop50ByEnfantIdOrderByCreeLeDesc(enfantId);
        if (recents == null || recents.isEmpty()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (ObservationEnfant o : recents) {
            if (o == null) {
                continue;
            }
            String t = o.getType() != null ? o.getType().name() : "";
            String titre = safe(o.getTitre());
            if (titre.isBlank()) {
                continue;
            }
            out.add(t + " - " + titre);
            if (out.size() >= 5) {
                break;
            }
        }
        return out;
    }

    private List<String> buildProblemesPossibles(ObservationType type,
                                                 NiveauUrgence urgence,
                                                 Double temperature,
                                                 String symptomes,
                                                 String contexte,
                                                 String allergies,
                                                 List<String> contexteRecent) {
        List<String> out = new ArrayList<>();
        String text = (safe(symptomes) + " " + safe(contexte)).toLowerCase();

        if (type == ObservationType.SANTE) {
            if (temperature != null && temperature >= 38.5) {
                out.add("Possible infection (fievre) ou inflammation. A surveiller.");
            }
            if (text.contains("toux") || text.contains("rhume") || text.contains("nez")) {
                out.add("Possible infection ORL (rhume/toux).");
            }
            if (text.contains("vom") || text.contains("diarr") || text.contains("ventre")) {
                out.add("Possible trouble digestif (gastro/intolerance). Hydratation importante.");
            }
            if (!safe(allergies).isBlank() && (text.contains("bouton") || text.contains("urtica") || text.contains("demange"))) {
                out.add("Possible reaction allergique (vu allergies connues).");
            }
        }

        if (type == ObservationType.SOMMEIL) {
            out.add("Possible manque de sommeil (nuit agitee) ou besoin de routine.");
            if (contexteRecent.stream().anyMatch(s -> s.contains("HUMEUR") || s.contains("COMPORTEMENT"))) {
                out.add("Lien possible avec humeur/comportement recents (fatigue).");
            }
        }

        if (type == ObservationType.ALIMENTATION) {
            out.add("Possible manque d'appetit (fatigue, stress, debut de maladie).");
            if (!safe(allergies).isBlank()) {
                out.add("Verifier allergenes / intolérances (allergies connues).");
            }
        }

        if (type == ObservationType.JOUET || type == ObservationType.ACTIVITE) {
            out.add("Possible frustration (partage, tour de role) ou besoin d'encadrement.");
            if (contexteRecent.stream().anyMatch(s -> s.contains("COMPORTEMENT"))) {
                out.add("Comportement similaire deja observe recemment (voir contexte recent).");
            }
        }

        if (type == ObservationType.COMPORTEMENT || type == ObservationType.HUMEUR) {
            out.add("Possible besoin de reassurance, transition difficile ou fatigue.");
            if (contexteRecent.stream().anyMatch(s -> s.contains("SOMMEIL"))) {
                out.add("Lien possible avec le sommeil recent.");
            }
        }

        if (type == ObservationType.INCIDENT) {
            out.add("Surveiller douleur/gonflement. Si aggravation: avis medical.");
        }

        if (urgence == NiveauUrgence.CRITIQUE || urgence == NiveauUrgence.ELEVEE) {
            out.add("Niveau d'urgence eleve: contacter le parent rapidement.");
        }

        // Ensure at least 2 items
        if (out.isEmpty()) {
            out.add("A determiner selon evolution et contexte.");
            out.add("Verifier si un evenement recent a pu declencher ce changement.");
        }
        return out;
    }
}
