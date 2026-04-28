package com.tinyspring.garderie.services;

import com.tinyspring.garderie.interfaces.IGroupeService;
import org.springframework.stereotype.Service;
import com.tinyspring.garderie.entity.Groupe;
import com.tinyspring.garderie.repository.GroupeRepository;
import com.tinyspring.garderie.repository.AffectationRepository;
import com.tinyspring.garderie.dto.ChildMatchingRequest;
import com.tinyspring.garderie.dto.GroupeSuggestionDTO;

import java.util.ArrayList;
import java.util.Comparator;

import java.util.List;

@Service
public class GroupeServiceImpl implements IGroupeService {

    private final GroupeRepository groupeRepository;
    private final AffectationRepository affectationRepository;

    public GroupeServiceImpl(GroupeRepository groupeRepository, AffectationRepository affectationRepository) {
        this.groupeRepository = groupeRepository;
        this.affectationRepository = affectationRepository;
    }

    private void validateGroupe(Groupe groupe) {
        // Vérifier que l'horaire de début est avant l'horaire de fin
        if (groupe.getHoraireDebut() != null && groupe.getHoraireFin() != null
                && !groupe.getHoraireDebut().isBefore(groupe.getHoraireFin())) {
            throw new IllegalArgumentException("L'horaire de début (" + groupe.getHoraireDebut()
                    + ") doit être avant l'horaire de fin (" + groupe.getHoraireFin() + ").");
        }

        // Vérifier que la capacité du groupe ne dépasse pas la capacité max de la classe
        if (groupe.getClasse() != null && groupe.getClasse().getCapaciteMax() != null
                && groupe.getCapacite() != null
                && groupe.getCapacite() > groupe.getClasse().getCapaciteMax()) {
            throw new IllegalArgumentException("La capacité du groupe (" + groupe.getCapacite()
                    + ") dépasse la capacité maximale de la classe '"
                    + groupe.getClasse().getNom() + "' (" + groupe.getClasse().getCapaciteMax() + ").");
        }

        // Anti-Collision Algorithm: Overlap Detection
        if (groupe.getHoraireDebut() != null && groupe.getHoraireFin() != null) {
            List<Groupe> allGroupes = groupeRepository.findAll();
            for (Groupe existing : allGroupes) {
                // Ignore self during updates
                if (groupe.getId() != null && groupe.getId().equals(existing.getId())) {
                    continue;
                }

                if (existing.getHoraireDebut() != null && existing.getHoraireFin() != null) {
                    boolean overlaps = groupe.getHoraireDebut().isBefore(existing.getHoraireFin()) &&
                                     groupe.getHoraireFin().isAfter(existing.getHoraireDebut());

                    if (overlaps) {
                        // Conflit 1 : Même Animatrice en même temps
                        if (groupe.getAnimatriceId() != null && groupe.getAnimatriceId().equals(existing.getAnimatriceId())) {
                            throw new IllegalArgumentException("Conflit Temporel : L'animatrice (ID " + groupe.getAnimatriceId() + 
                            ") est déjà affectée au groupe '" + existing.getNom() + "' de " + 
                            existing.getHoraireDebut() + " à " + existing.getHoraireFin() + ".");
                        }

                        // Conflit 2 : Même Salle en même temps
                        if (groupe.getClasse() != null && groupe.getClasse().getSalle() != null &&
                            existing.getClasse() != null && existing.getClasse().getSalle() != null) {
                            
                            if (groupe.getClasse().getSalle().getId().equals(existing.getClasse().getSalle().getId())) {
                                throw new IllegalArgumentException("Conflit Temporel : La salle '" + groupe.getClasse().getSalle().getNom() + 
                                "' est déjà occupée par le groupe '" + existing.getNom() + "' de " + 
                                existing.getHoraireDebut() + " à " + existing.getHoraireFin() + ".");
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public Groupe addGroupe(Groupe groupe) {
        validateGroupe(groupe);
        return groupeRepository.save(groupe);
    }

    @Override
    public Groupe updateGroupe(Groupe groupe) {
        validateGroupe(groupe);
        return groupeRepository.save(groupe);
    }

    @Override
    public void deleteGroupe(Long id) {
        groupeRepository.deleteById(id);
    }

    @Override
    public Groupe getGroupeById(Long id) {
        return groupeRepository.findById(id).orElse(null);
    }

    @Override
    public List<Groupe> getAllGroupes() {
        return groupeRepository.findAll();
    }

    @Override
    public List<GroupeSuggestionDTO> suggestGroups(ChildMatchingRequest request) {
        List<Groupe> allGroupes = groupeRepository.findAll();
        List<GroupeSuggestionDTO> suggestions = new ArrayList<>();

        for (Groupe groupe : allGroupes) {
            List<String> matchReasons = new ArrayList<>();
            double matchPercentage = 0.0;

            // 1. Filtre Strict : Age Constraint
            if (request.getAge() != null && groupe.getClasse() != null) {
                Integer minAge = groupe.getClasse().getAgeMinimum();
                Integer maxAge = groupe.getClasse().getAgeMaximum();
                if ((minAge != null && request.getAge() < minAge) || 
                    (maxAge != null && request.getAge() > maxAge)) {
                    continue; // Incompatible age
                }
            }

            // 2. Filtre Strict : Capacité
            int enrolledCount = affectationRepository.countByGroupeIdAndStatut(groupe.getId(), "ACTIF");
            int availableSeats = groupe.getCapacite() - enrolledCount;
            if (availableSeats <= 0) {
                continue; // No seats left
            }

            // 3. Calcul du Score : Load Balancing (60% weight)
            double loadPercentage = (double) availableSeats / groupe.getCapacite();
            double loadScore = loadPercentage * 60.0;
            matchPercentage += loadScore;
            
            if (loadPercentage > 0.5) {
                matchReasons.add("Excellente disponibilité (" + availableSeats + " places)");
            } else {
                matchReasons.add("Places limitées (" + availableSeats + " places)");
            }

            // 4. Calcul du Score : Langue preference (40% weight)
            if (request.getLanguePrincipale() != null && !request.getLanguePrincipale().isEmpty()) {
                if (request.getLanguePrincipale().equalsIgnoreCase(groupe.getLanguePrincipale())) {
                    matchPercentage += 40.0;
                    matchReasons.add("Correspondance parfaite de la langue (" + groupe.getLanguePrincipale() + ")");
                }
            } else {
                // If language isn't specified, grant proportional points to not penalize
                matchPercentage += 40.0; 
            }

            // Round to 1 decimal
            matchPercentage = Math.round(matchPercentage * 10.0) / 10.0;

            suggestions.add(GroupeSuggestionDTO.builder()
                    .groupe(groupe)
                    .matchPercentage(matchPercentage)
                    .availableSeats(availableSeats)
                    .matchReasons(matchReasons)
                    .build());
        }

        // Trier par pourcentage décroissant
        suggestions.sort(Comparator.comparing(GroupeSuggestionDTO::getMatchPercentage).reversed());

        // Garder le Top 3
        return suggestions.size() > 3 ? suggestions.subList(0, 3) : suggestions;
    }
}
