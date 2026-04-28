package com.tinyspring.garderie.service.RH;

import com.tinyspring.garderie.dto.RH.RapportRHDTO;
import com.tinyspring.garderie.dto.RH.mapper.RapportRHMapper;
import com.tinyspring.garderie.entity.RH.AbsenceConge;
import com.tinyspring.garderie.entity.RH.RapportRH;
import com.tinyspring.garderie.entity.RH.enums.StatutAbsenceConge;
import com.tinyspring.garderie.entity.RH.enums.TypeAbsenceConge;
import com.tinyspring.garderie.repository.RH.AbsenceCongeRepository;
import com.tinyspring.garderie.repository.RH.AnimatriceRepository;
import com.tinyspring.garderie.repository.RH.RapportRHRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RapportRHServiceImpl implements IRapportRHService {

    private final IOllamaService ollamaService;
    private final RapportRHRepository rapportRHRepository;
    private final AbsenceCongeRepository absenceCongeRepository;
    private final AnimatriceRepository animatriceRepository;

    // ✅ MapStruct mapper injecté
    private final RapportRHMapper rapportRHMapper;

    @Override
    public RapportRHDTO genererRapport(String question) {
        String donneesContexte = collecterDonnees();
        String prompt = construirePrompt(question, donneesContexte);
        String contenuRapport = ollamaService.generer(prompt);

        RapportRH rapport = RapportRH.builder()
                .question(question)
                .typeRapport(detecterTypeRapport(question))
                .periode(detecterPeriode(question))
                .contenu(contenuRapport)
                .donneesContexte(donneesContexte)
                .build();

        return rapportRHMapper.toDTO(rapportRHRepository.save(rapport));
    }

    @Override
    public List<RapportRHDTO> getTousLesRapports() {
        return rapportRHRepository.findAllByOrderByDateGenerationDesc().stream()
                .map(rapportRHMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public RapportRHDTO getRapportById(Long id) {
        return rapportRHRepository.findById(id)
                .map(rapportRHMapper::toDTO)
                .orElseThrow(() -> new RuntimeException("Rapport non trouvé : " + id));
    }

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
        sb.append("ANIMATRICES\nTotal : ").append(totalAnimatrices)
                .append("\nActives : ").append(actives)
                .append("\nInactives : ").append(inactives).append("\n\n");

        List<AbsenceConge> absences = absenceCongeRepository.findAll();
        long enAttente  = absences.stream().filter(a -> StatutAbsenceConge.EN_ATTENTE.equals(a.getStatut())).count();
        long approuvees = absences.stream().filter(a -> StatutAbsenceConge.APPROUVE.equals(a.getStatut())).count();
        long refusees   = absences.stream().filter(a -> StatutAbsenceConge.REFUSE.equals(a.getStatut())).count();

        sb.append("ABSENCES\nTotal : ").append(absences.size())
                .append("\nApprouvées : ").append(approuvees)
                .append("\nRefusées : ").append(refusees)
                .append("\nEn attente : ").append(enAttente).append("\n\n");

        sb.append("PAR TYPE\n");
        for (TypeAbsenceConge type : TypeAbsenceConge.values()) {
            long count = absences.stream().filter(a -> type.equals(a.getType())).count();
            int totalJours = absences.stream()
                    .filter(a -> type.equals(a.getType()))
                    .filter(a -> !StatutAbsenceConge.REFUSE.equals(a.getStatut()))
                    .mapToInt(a -> a.getNbJours() != null ? a.getNbJours() : 0).sum();
            sb.append(type.name()).append(" : ").append(count)
                    .append(" demandes, ").append(totalJours).append(" jours\n");
        }
        return sb.toString();
    }

    private String construirePrompt(String question, String donnees) {
        return "Tu es TinySpring-RH, un assistant expert en gestion RH.\n\n" +
                "### Instruction:\n" + question + "\n\n" +
                "### Input:\n" + donnees + "\n\n" +
                "### Response:\n";
    }

    private String detecterTypeRapport(String question) {
        String q = question.toLowerCase();
        if (q.contains("mensuel") || q.contains("mois")) return "MENSUEL";
        if (q.contains("trimestriel"))  return "TRIMESTRIEL";
        if (q.contains("annuel"))       return "ANNUEL";
        if (q.contains("absence") || q.contains("congé")) return "ABSENCES";
        if (q.contains("formation"))    return "FORMATIONS";
        if (q.contains("animatrice"))   return "ANIMATRICES";
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
        if (q.contains("mois"))    return "Mois en cours";
        return "Période générale";
    }
}
