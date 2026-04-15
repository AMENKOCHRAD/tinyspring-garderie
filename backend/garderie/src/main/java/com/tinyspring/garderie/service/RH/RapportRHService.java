package com.tinyspring.garderie.service.RH;

import com.tinyspring.garderie.dto.RH.RapportRHDTO;
import com.tinyspring.garderie.entity.RH.AbsenceConge;
import com.tinyspring.garderie.entity.RH.RapportRH;
import com.tinyspring.garderie.entity.RH.enums.StatutAbsenceConge;
import com.tinyspring.garderie.entity.RH.enums.TypeAbsenceConge;
import com.tinyspring.garderie.repository.RH.AbsenceCongeRepository;
import com.tinyspring.garderie.repository.RH.AnimatriceRepository;
import com.tinyspring.garderie.repository.RH.FormationRepository;
import com.tinyspring.garderie.repository.RH.RapportRHRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RapportRHService {

    private final OllamaService ollamaService; // ✅ Ollama local
    private final RapportRHRepository rapportRHRepository;
    private final AbsenceCongeRepository absenceCongeRepository;
    private final AnimatriceRepository animatriceRepository;
    private final FormationRepository formationRepository;

    /**
     * Génère un rapport RH à partir d'une question en langage naturel
     */
    public RapportRHDTO genererRapport(String question) {

        // 1. Collecter toutes les données temps réel
        String donneesContexte = collecterDonnees();

        // 2. Construire le prompt format Alpaca
        String prompt = construirePrompt(question, donneesContexte);

        // 3. Appeler Ollama local
        String contenuRapport = ollamaService.generer(prompt);

        // 4. Sauvegarder le rapport
        RapportRH rapport = RapportRH.builder()
                .question(question)
                .typeRapport(detecterTypeRapport(question))
                .periode(detecterPeriode(question))
                .contenu(contenuRapport)
                .donneesContexte(donneesContexte)
                .build();

        RapportRH saved = rapportRHRepository.save(rapport);
        return toDTO(saved);
    }

    /**
     * Collecte toutes les données RH temps réel
     */
    private String collecterDonnees() {
        StringBuilder sb = new StringBuilder();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDate maintenant = LocalDate.now();

        long totalAnimatrices = animatriceRepository.count();
        long actives = animatriceRepository.findByStatut(
                com.tinyspring.garderie.entity.RH.enums.StatutAnimatrice.ACTIVE).size();
        long inactives = totalAnimatrices - actives;

        sb.append("=== DONNÉES RH TINYSPRING GARDERIE ===\n");
        sb.append("Date : ").append(maintenant.format(fmt)).append("\n\n");

        sb.append("ANIMATRICES\n");
        sb.append("Total : ").append(totalAnimatrices).append("\n");
        sb.append("Actives : ").append(actives).append("\n");
        sb.append("Inactives : ").append(inactives).append("\n\n");

        List<AbsenceConge> absences = absenceCongeRepository.findAll();

        long enAttente = absences.stream()
                .filter(a -> StatutAbsenceConge.EN_ATTENTE.equals(a.getStatut())).count();
        long approuvees = absences.stream()
                .filter(a -> StatutAbsenceConge.APPROUVE.equals(a.getStatut())).count();
        long refusees = absences.stream()
                .filter(a -> StatutAbsenceConge.REFUSE.equals(a.getStatut())).count();
        long autoApprouvees = absences.stream()
                .filter(a -> StatutAbsenceConge.APPROUVE.equals(a.getStatut()))
                .filter(a -> Boolean.TRUE.equals(a.getDecisionAutomatique())).count();
        long autoRefusees = absences.stream()
                .filter(a -> StatutAbsenceConge.REFUSE.equals(a.getStatut()))
                .filter(a -> Boolean.TRUE.equals(a.getDecisionAutomatique())).count();

        sb.append("ABSENCES\n");
        sb.append("Total : ").append(absences.size()).append("\n");
        sb.append("Approuvées : ").append(approuvees)
                .append(" (dont ").append(autoApprouvees).append(" automatiques)\n");
        sb.append("Refusées : ").append(refusees)
                .append(" (dont ").append(autoRefusees).append(" automatiques)\n");
        sb.append("En attente : ").append(enAttente).append("\n\n");

        sb.append("PAR TYPE\n");
        for (TypeAbsenceConge type : TypeAbsenceConge.values()) {
            long count = absences.stream()
                    .filter(a -> type.equals(a.getType())).count();
            int totalJours = absences.stream()
                    .filter(a -> type.equals(a.getType()))
                    .filter(a -> !StatutAbsenceConge.REFUSE.equals(a.getStatut()))
                    .mapToInt(a -> a.getNbJours() != null ? a.getNbJours() : 0)
                    .sum();
            sb.append(type.name()).append(" : ").append(count)
                    .append(" demandes, ").append(totalJours).append(" jours\n");
        }

        // Demandes en attente
        sb.append("\nDEMANDES EN ATTENTE\n");
        absences.stream()
                .filter(a -> StatutAbsenceConge.EN_ATTENTE.equals(a.getStatut()))
                .limit(5)
                .forEach(a -> sb.append("• ")
                        .append(a.getAnimatrice().getPrenom()).append(" ")
                        .append(a.getAnimatrice().getNom())
                        .append(" | ").append(a.getType().name())
                        .append(" | ").append(a.getNbJours()).append(" jours\n"));

        // Formations
        long totalFormations = formationRepository.count();
        sb.append("\nFORMATIONS\n");
        sb.append("Total : ").append(totalFormations).append("\n");

        return sb.toString();
    }

    /**
     * Construit le prompt au format Alpaca (même format que le fine-tuning)
     */
    private String construirePrompt(String question, String donnees) {
        return "Tu es TinySpring-RH, un assistant expert en gestion RH pour TinySpring Garderie.\n\n" +
                "### Instruction:\n" +
                question + "\n\n" +
                "### Input:\n" +
                donnees + "\n\n" +
                "### Response:\n";
    }

    private String detecterTypeRapport(String question) {
        String q = question.toLowerCase();
        if (q.contains("mensuel") || q.contains("mois")) return "MENSUEL";
        if (q.contains("trimestriel") || q.contains("trimestre")) return "TRIMESTRIEL";
        if (q.contains("annuel") || q.contains("année")) return "ANNUEL";
        if (q.contains("absence") || q.contains("congé")) return "ABSENCES";
        if (q.contains("formation")) return "FORMATIONS";
        if (q.contains("animatrice")) return "ANIMATRICES";
        if (q.contains("alerte") || q.contains("quota")) return "ALERTES";
        return "GENERAL";
    }

    private String detecterPeriode(String question) {
        String q = question.toLowerCase();
        String[] mois = {"janvier", "février", "mars", "avril", "mai", "juin",
                "juillet", "août", "septembre", "octobre", "novembre", "décembre"};
        for (String m : mois) {
            if (q.contains(m))
                return m.substring(0, 1).toUpperCase() + m.substring(1);
        }
        if (q.contains("semaine")) return "Semaine en cours";
        if (q.contains("mois")) return "Mois en cours";
        return "Période générale";
    }

    public List<RapportRHDTO> getTousLesRapports() {
        return rapportRHRepository.findAllByOrderByDateGenerationDesc()
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public RapportRHDTO getRapportById(Long id) {
        return rapportRHRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("Rapport non trouvé : " + id));
    }

    private RapportRHDTO toDTO(RapportRH rapport) {
        return RapportRHDTO.builder()
                .id(rapport.getId())
                .question(rapport.getQuestion())
                .typeRapport(rapport.getTypeRapport())
                .periode(rapport.getPeriode())
                .contenu(rapport.getContenu())
                .dateGeneration(rapport.getDateGeneration())
                .build();
    }
}