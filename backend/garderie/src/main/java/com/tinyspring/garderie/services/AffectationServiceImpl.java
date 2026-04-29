package com.tinyspring.garderie.services;

import org.springframework.stereotype.Service;
import com.tinyspring.garderie.entity.Affectation;
import com.tinyspring.garderie.repository.AffectationRepository;
import com.tinyspring.garderie.interfaces.IAffectationService;

import java.util.List;

@Service
public class AffectationServiceImpl implements IAffectationService {

    private final AffectationRepository affectationRepository;

    public AffectationServiceImpl(AffectationRepository affectationRepository) {
        this.affectationRepository = affectationRepository;
    }

    private void validateAffectation(Affectation affectation) {
        // Vérifier que la date de début est avant la date de fin
        if (affectation.getDateDebut() != null && affectation.getDateFin() != null
                && !affectation.getDateDebut().isBefore(affectation.getDateFin())) {
            throw new IllegalArgumentException("La date de début (" + affectation.getDateDebut()
                    + ") doit être antérieure à la date de fin (" + affectation.getDateFin() + ").");
        }

        // Vérifier que le statut est valide
        if (affectation.getStatut() != null) {
            String statut = affectation.getStatut().toUpperCase();
            if (!statut.equals("ACTIF") && !statut.equals("SUSPENDU") && !statut.equals("TERMINE")) {
                throw new IllegalArgumentException("Le statut '" + affectation.getStatut()
                        + "' n'est pas valide. Valeurs autorisées : ACTIF, SUSPENDU, TERMINE.");
            }
        }
    }

    @Override
    public Affectation addAffectation(Affectation affectation) {
        validateAffectation(affectation);
        return affectationRepository.save(affectation);
    }

    @Override
    public Affectation updateAffectation(Affectation affectation) {
        validateAffectation(affectation);
        return affectationRepository.save(affectation);
    }

    @Override
    public void deleteAffectation(Long id) {
        affectationRepository.deleteById(id);
    }

    @Override
    public Affectation getAffectationById(Long id) {
        return affectationRepository.findById(id).orElse(null);
    }

    @Override
    public List<Affectation> getAllAffectations() {
        return affectationRepository.findAll();
    }
}
