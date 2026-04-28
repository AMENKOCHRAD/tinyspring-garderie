package com.tinyspring.garderie.services;

import com.tinyspring.garderie.dto.DashboardStatsDTO;
import com.tinyspring.garderie.entity.Affectation;
import com.tinyspring.garderie.entity.Groupe;
import com.tinyspring.garderie.repository.AffectationRepository;
import com.tinyspring.garderie.repository.ClasseRepository;
import com.tinyspring.garderie.repository.GroupeRepository;
import com.tinyspring.garderie.repository.SalleRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final SalleRepository salleRepository;
    private final ClasseRepository classeRepository;
    private final GroupeRepository groupeRepository;
    private final AffectationRepository affectationRepository;

    public DashboardService(SalleRepository salleRepository, ClasseRepository classeRepository, 
                            GroupeRepository groupeRepository, AffectationRepository affectationRepository) {
        this.salleRepository = salleRepository;
        this.classeRepository = classeRepository;
        this.groupeRepository = groupeRepository;
        this.affectationRepository = affectationRepository;
    }

    public DashboardStatsDTO getStats() {
        long totalSalles = salleRepository.count();
        long totalClasses = classeRepository.count();
        long totalGroupes = groupeRepository.count();

        List<Affectation> allAffectations = affectationRepository.findAll();
        long totalEnfantsActifs = allAffectations.stream().filter(a -> "ACTIF".equals(a.getStatut())).count();

        List<Groupe> allGroupesList = groupeRepository.findAll();
        
        // Calcul Taux Occupation Global : Total des enfants / Somme de toutes les capacités des groupes
        long totalCapaciteGarderie = allGroupesList.stream().mapToLong(Groupe::getCapacite).sum();
        double globalOccupancyRate = 0;
        if (totalCapaciteGarderie > 0) {
            globalOccupancyRate = ((double) totalEnfantsActifs / totalCapaciteGarderie) * 100.0;
        }

        // Répartition des langues
        Map<String, Long> repartitionsLangues = allGroupesList.stream()
                .filter(g -> g.getLanguePrincipale() != null && !g.getLanguePrincipale().trim().isEmpty())
                .collect(Collectors.groupingBy(Groupe::getLanguePrincipale, Collectors.counting()));

        // Enfants par salle
        Map<String, Long> enfantsParSalle = new HashMap<>();
        allAffectations.stream().filter(a -> "ACTIF".equals(a.getStatut())).forEach(a -> {
            if (a.getGroupe() != null && a.getGroupe().getClasse() != null && a.getGroupe().getClasse().getSalle() != null) {
                String salleName = a.getGroupe().getClasse().getSalle().getNom();
                enfantsParSalle.put(salleName, enfantsParSalle.getOrDefault(salleName, 0L) + 1);
            }
        });

        // Arrondir occupancy rate
        globalOccupancyRate = Math.round(globalOccupancyRate * 10.0) / 10.0;

        return DashboardStatsDTO.builder()
                .totalSalles(totalSalles)
                .totalClasses(totalClasses)
                .totalGroupes(totalGroupes)
                .totalEnfantsActifs(totalEnfantsActifs)
                .globalOccupancyRate(globalOccupancyRate)
                .repartitionsLangues(repartitionsLangues)
                .enfantsParSalle(enfantsParSalle)
                .build();
    }
}
