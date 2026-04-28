package com.tinyspring.garderie.services;

import com.tinyspring.garderie.interfaces.IClasseService;
import org.springframework.stereotype.Service;
import com.tinyspring.garderie.entity.Classe;
import com.tinyspring.garderie.repository.ClasseRepository;

import java.util.List;

@Service
public class ClasseServiceImpl implements IClasseService {

    private final ClasseRepository classeRepository;

    public ClasseServiceImpl(ClasseRepository classeRepository) {
        this.classeRepository = classeRepository;
    }

    private void validateClasse(Classe classe) {
        // Vérifier que l'âge minimum < âge maximum
        if (classe.getAgeMinimum() != null && classe.getAgeMaximum() != null
                && classe.getAgeMinimum() >= classe.getAgeMaximum()) {
            throw new IllegalArgumentException("L'âge minimum (" + classe.getAgeMinimum()
                    + ") doit être inférieur à l'âge maximum (" + classe.getAgeMaximum() + ").");
        }

        // Vérifier que la salle n'est pas fermée (indisponible)
        if (classe.getSalle() != null && classe.getSalle().getDisponible() != null
                && !classe.getSalle().getDisponible()) {
            throw new IllegalArgumentException("La salle '" + classe.getSalle().getNom()
                    + "' est actuellement fermée/indisponible. Veuillez choisir une salle en service.");
        }

        // Vérifier que la capacité de la classe ne dépasse pas une limite raisonnable par rapport à la surface
        // Règle : au moins 2 m² par enfant
        if (classe.getSalle() != null && classe.getCapaciteMax() != null) {
            double surfaceSalle = classe.getSalle().getSurface();
            int capaciteParSurface = (int) (surfaceSalle / 2);
            if (classe.getCapaciteMax() > capaciteParSurface) {
                throw new IllegalArgumentException("La capacité de " + classe.getCapaciteMax()
                        + " enfants est trop élevée pour la salle '" + classe.getSalle().getNom()
                        + "' (" + surfaceSalle + " m²). Maximum recommandé : " + capaciteParSurface
                        + " enfants (2 m²/enfant).");
            }
        }
    }

    @Override
    public Classe addClasse(Classe classe) {
        validateClasse(classe);
        return classeRepository.save(classe);
    }

    @Override
    public Classe updateClasse(Classe classe) {
        validateClasse(classe);
        return classeRepository.save(classe);
    }

    @Override
    public void deleteClasse(Long id) {
        classeRepository.deleteById(id);
    }

    @Override
    public Classe getClasseById(Long id) {
        return classeRepository.findById(id).orElse(null);
    }

    @Override
    public List<Classe> getAllClasses() {
        return classeRepository.findAll();
    }
}
