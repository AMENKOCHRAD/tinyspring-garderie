package com.tinyspring.garderie.service.Events;

import com.tinyspring.garderie.dto.Events.EventRecommendationCandidateDto;
import com.tinyspring.garderie.dto.Events.EventRecommendationContextRequest;
import com.tinyspring.garderie.dto.Events.EventRecommendationPythonRequest;
import com.tinyspring.garderie.dto.Events.EventRecommendationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventRecommendationServiceImpl implements EventRecommendationService {

    private final RestTemplate restTemplate;

    @Value("${ai.service.events.url:http://localhost:5006}")
    private String pythonAiUrl;

    @Override
    public List<EventRecommendationResponse> recommendEvents(EventRecommendationContextRequest request) {
        try {
            List<EventRecommendationCandidateDto> candidates = buildCandidates(request);

            EventRecommendationPythonRequest payload = EventRecommendationPythonRequest.builder()
                    .season(request.getSeason())
                    .month(request.getMonth())
                    .ageGroup(request.getAgeGroup())
                    .budgetLevel(request.getBudgetLevel())
                    .outdoorPreferred(request.getOutdoorPreferred())
                    .cityContext(request.getCityContext())
                    .candidates(candidates)
                    .build();

            System.out.println("=== SPRING -> PYTHON URL ===");
            System.out.println(pythonAiUrl + "/recommend-events");

            System.out.println("=== SPRING -> PYTHON PAYLOAD ===");
            System.out.println(payload);

            ResponseEntity<EventRecommendationResponse[]> response =
                    restTemplate.postForEntity(
                            pythonAiUrl + "/recommend-events",
                            payload,
                            EventRecommendationResponse[].class
                    );

            System.out.println("=== PYTHON RESPONSE STATUS ===");
            System.out.println(response.getStatusCode());

            System.out.println("=== PYTHON RESPONSE BODY ===");
            System.out.println(response.getBody());

            if (response.getBody() == null) {
                return List.of();
            }

            return List.of(response.getBody());

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.out.println("=== PYTHON CLIENT ERROR ===");
            System.out.println("Status: " + e.getStatusCode());
            System.out.println("Body: " + e.getResponseBodyAsString());
            throw new IllegalStateException("Erreur HTTP depuis Python: " + e.getResponseBodyAsString(), e);

        } catch (org.springframework.web.client.HttpServerErrorException e) {
            System.out.println("=== PYTHON SERVER ERROR ===");
            System.out.println("Status: " + e.getStatusCode());
            System.out.println("Body: " + e.getResponseBodyAsString());
            throw new IllegalStateException("Erreur serveur Python: " + e.getResponseBodyAsString(), e);

        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException("Impossible d'obtenir les recommandations IA", e);
        }
    }

    private List<EventRecommendationCandidateDto> buildCandidates(EventRecommendationContextRequest request) {
        String city = normalize(request.getCityContext());
        String season = normalize(request.getSeason());
        String budget = normalize(request.getBudgetLevel());
        boolean outdoorPreferred = Boolean.TRUE.equals(request.getOutdoorPreferred());

        List<EventRecommendationCandidateDto> candidates = new ArrayList<>();

        // TUNIS / ARIANA / GRAND TUNIS
        if (containsAny(city, "tunis", "ariana", "ben arous", "manouba", "grand tunis")) {
            candidates.add(candidate("Exploration nature au Belvédère",
                    "Balade éducative, observation des arbres et découverte du monde vivant.",
                    "SORTIE", "Parc du Belvédère, Tunis, Tunisie", "OUTDOOR", true, 12.0));

            candidates.add(candidate("Atelier sciences ludiques",
                    "Petites expériences adaptées aux enfants dans un cadre éducatif sécurisé.",
                    "ATELIER", "Cité des Sciences, Tunis, Tunisie", "INDOOR", false, 10.0));

            candidates.add(candidate("Jeux en plein air",
                    "Activités motrices, jeux collectifs et découverte de la nature.",
                    "ACTIVITE", "Parc Ennahli, Ariana, Tunisie", "OUTDOOR", true, 8.0));

            candidates.add(candidate("Visite découverte de la mer",
                    "Activité sensorielle autour de la mer et des animaux marins.",
                    "SORTIE", "Musée Océanographique de Salammbô, La Goulette, Tunisie", "INDOOR", true, 14.0));

            candidates.add(candidate("Journée festive au jardin",
                    "Animation, musique douce, jeux et goûter dans un espace vert.",
                    "FETE", "Jardin Japonais, Tunis, Tunisie", "OUTDOOR", false, 9.0));

            candidates.add(candidate("Rencontre parents-équipe",
                    "Temps d’échange structuré avec les familles et présentation des activités.",
                    "REUNION_PARENTS", "Maison de la Culture Ibn Rachiq, Tunis, Tunisie", "INDOOR", false, 0.0));
        }

        // SOUSSE / MONASTIR / MAHDIA
        if (containsAny(city, "sousse", "monastir", "mahdia", "sahel")) {
            candidates.add(candidate("Journée plage éducative",
                    "Jeux de sable, découverte du littoral et activités motrices.",
                    "SORTIE", "Plage de Boujaafar, Sousse, Tunisie", "OUTDOOR", true, 11.0));

            candidates.add(candidate("Atelier créatif culturel",
                    "Peinture, collage et motricité fine dans un cadre calme.",
                    "ATELIER", "Complexe culturel de Sousse, Sousse, Tunisie", "INDOOR", false, 7.0));

            candidates.add(candidate("Sortie détente au parc",
                    "Jeux collectifs et parcours moteur pour les petits.",
                    "ACTIVITE", "Parc Hannibal, Sousse, Tunisie", "OUTDOOR", false, 6.0));

            candidates.add(candidate("Activité aquatique encadrée",
                    "Découverte ludique de l’eau avec animation adaptée.",
                    "SORTIE", "AquaSplash Thalassa Sousse, Sousse, Tunisie", "OUTDOOR", true, 18.0));

            candidates.add(candidate("Réunion familles",
                    "Échange sur le développement de l’enfant et les activités pédagogiques.",
                    "REUNION_PARENTS", "Maison de la Culture, Sousse, Tunisie", "INDOOR", false, 0.0));
        }

        // SFAX
        if (containsAny(city, "sfax")) {
            candidates.add(candidate("Jeux et motricité au parc",
                    "Parcours moteur, jeux de groupe et respiration en plein air.",
                    "ACTIVITE", "Parc Touta, Sfax, Tunisie", "OUTDOOR", false, 6.0));

            candidates.add(candidate("Atelier musique et rythme",
                    "Découverte des sons, comptines et expression corporelle.",
                    "ATELIER", "Centre culturel Mohamed Jamoussi, Sfax, Tunisie", "INDOOR", false, 8.0));

            candidates.add(candidate("Sortie nature découverte",
                    "Observation de l’environnement et activités éducatives en extérieur.",
                    "SORTIE", "Ain Park, Sfax, Tunisie", "OUTDOOR", true, 10.0));

            candidates.add(candidate("Réunion pédagogique familles",
                    "Présentation du projet éducatif et échanges avec les parents.",
                    "REUNION_PARENTS", "Maison de la Culture, Sfax, Tunisie", "INDOOR", false, 0.0));
        }

        // NABEUL / HAMMAMET
        if (containsAny(city, "nabeul", "hammamet", "cap bon")) {
            candidates.add(candidate("Promenade découverte en médina",
                    "Découverte visuelle et sensorielle d’un cadre culturel vivant.",
                    "SORTIE", "Médina de Hammamet, Hammamet, Tunisie", "OUTDOOR", true, 10.0));

            candidates.add(candidate("Atelier artistique",
                    "Créativité, dessin et couleurs dans un cadre culturel.",
                    "ATELIER", "Centre culturel international de Hammamet, Hammamet, Tunisie", "INDOOR", false, 9.0));

            candidates.add(candidate("Jeux de plein air",
                    "Animation ludique et coopérative dans une zone touristique sécurisée.",
                    "ACTIVITE", "Yasmine Hammamet, Hammamet, Tunisie", "OUTDOOR", true, 12.0));

            candidates.add(candidate("Réunion parents",
                    "Discussion sur les progrès des enfants et prochaines activités.",
                    "REUNION_PARENTS", "Maison de la Culture, Nabeul, Tunisie", "INDOOR", false, 0.0));
        }

        // BIZERTE
        if (containsAny(city, "bizerte")) {
            candidates.add(candidate("Sortie bord de mer",
                    "Découverte de l’environnement marin et jeux adaptés aux enfants.",
                    "SORTIE", "Corniche de Bizerte, Bizerte, Tunisie", "OUTDOOR", true, 9.0));

            candidates.add(candidate("Atelier manuel",
                    "Bricolage simple, collage et créativité.",
                    "ATELIER", "Maison de la Culture, Bizerte, Tunisie", "INDOOR", false, 6.0));

            candidates.add(candidate("Jeux au parc",
                    "Moments de détente et motricité libre en extérieur.",
                    "ACTIVITE", "Parc public de Bizerte, Bizerte, Tunisie", "OUTDOOR", false, 5.0));
        }

        // FALLBACK TUNISIE
        if (candidates.isEmpty()) {
            candidates.add(candidate("Atelier jardinage",
                    "Plantation, découverte des plantes et éveil sensoriel.",
                    "ATELIER", "Cité des Sciences, Tunis, Tunisie", "INDOOR", false, 8.0));

            candidates.add(candidate("Sortie nature en parc",
                    "Découverte des fleurs, arbres et jeux éducatifs.",
                    "SORTIE", "Parc du Belvédère, Tunis, Tunisie", "OUTDOOR", true, 12.0));

            candidates.add(candidate("Jeux collectifs",
                    "Activités de groupe et motricité dans un environnement sécurisé.",
                    "ACTIVITE", "Parc Ennahli, Ariana, Tunisie", "OUTDOOR", false, 7.0));

            candidates.add(candidate("Réunion parents",
                    "Temps d’échange avec les familles sur les activités et l’évolution des enfants.",
                    "REUNION_PARENTS", "Maison de la Culture Ibn Rachiq, Tunis, Tunisie", "INDOOR", false, 0.0));
        }

        candidates = applySeasonAdjustments(candidates, season);
        candidates = applyBudgetAdjustments(candidates, budget);
        candidates = applyOutdoorPreference(candidates, outdoorPreferred);

        return candidates.stream()
                .sorted(Comparator.comparing(EventRecommendationCandidateDto::getSuggestedPrice))
                .limit(25)
                .collect(Collectors.toList());
    }

    private List<EventRecommendationCandidateDto> applySeasonAdjustments(
            List<EventRecommendationCandidateDto> candidates,
            String season
    ) {
        if (season.contains("hiver")) {
            return candidates.stream()
                    .sorted((a, b) -> scoreIndoorFirst(a).compareTo(scoreIndoorFirst(b)))
                    .collect(Collectors.toList());
        }

        if (season.contains("ete") || season.contains("été")) {
            return candidates.stream()
                    .sorted((a, b) -> scoreSummerFriendly(a).compareTo(scoreSummerFriendly(b)))
                    .collect(Collectors.toList());
        }

        return candidates;
    }

    private List<EventRecommendationCandidateDto> applyBudgetAdjustments(
            List<EventRecommendationCandidateDto> candidates,
            String budget
    ) {
        if (budget.contains("low") || budget.contains("faible")) {
            return candidates.stream()
                    .filter(c -> c.getSuggestedPrice() <= 10.0)
                    .collect(Collectors.toList());
        }

        if (budget.contains("medium") || budget.contains("moyen")) {
            return candidates.stream()
                    .filter(c -> c.getSuggestedPrice() <= 15.0)
                    .collect(Collectors.toList());
        }

        return candidates;
    }

    private List<EventRecommendationCandidateDto> applyOutdoorPreference(
            List<EventRecommendationCandidateDto> candidates,
            boolean outdoorPreferred
    ) {
        return candidates.stream()
                .sorted((a, b) -> {
                    int scoreA = outdoorMatchScore(a, outdoorPreferred);
                    int scoreB = outdoorMatchScore(b, outdoorPreferred);
                    return Integer.compare(scoreB, scoreA);
                })
                .collect(Collectors.toList());
    }

    private Integer scoreIndoorFirst(EventRecommendationCandidateDto dto) {
        return "INDOOR".equalsIgnoreCase(dto.getIndoorOutdoor()) ? 0 : 1;
    }

    private Integer scoreSummerFriendly(EventRecommendationCandidateDto dto) {
        if ("INDOOR".equalsIgnoreCase(dto.getIndoorOutdoor())) {
            return 0;
        }
        if (dto.getSuggestedLocation() != null &&
                dto.getSuggestedLocation().toLowerCase(Locale.ROOT).contains("plage")) {
            return 1;
        }
        return 2;
    }

    private int outdoorMatchScore(EventRecommendationCandidateDto dto, boolean outdoorPreferred) {
        boolean isOutdoor = "OUTDOOR".equalsIgnoreCase(dto.getIndoorOutdoor());
        return outdoorPreferred == isOutdoor ? 1 : 0;
    }

    private EventRecommendationCandidateDto candidate(
            String title,
            String description,
            String eventType,
            String location,
            String indoorOutdoor,
            boolean requiresAuthorization,
            double suggestedPrice
    ) {
        return EventRecommendationCandidateDto.builder()
                .title(title)
                .description(description)
                .eventType(eventType)
                .suggestedLocation(location)
                .indoorOutdoor(indoorOutdoor)
                .requiresAuthorization(requiresAuthorization)
                .suggestedPrice(suggestedPrice)
                .build();
    }

    private boolean containsAny(String source, String... values) {
        for (String value : values) {
            if (source.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }
}