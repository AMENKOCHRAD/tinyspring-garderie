package com.tinyspring.garderie.interfaces;

import com.tinyspring.garderie.entity.Affectation;
import java.util.List;

public interface IAffectationService {
    Affectation addAffectation(Affectation affectation);
    Affectation updateAffectation(Affectation affectation);
    void deleteAffectation(Long id);
    Affectation getAffectationById(Long id);
    List<Affectation> getAllAffectations();
}
