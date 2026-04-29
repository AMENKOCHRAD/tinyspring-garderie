package com.tinyspring.garderie.interfaces;

import com.tinyspring.garderie.entity.Salle;
import java.util.List;

public interface ISalleService {
    Salle addSalle(Salle salle);
    Salle updateSalle(Salle salle);
    void deleteSalle(Long id);
    Salle getSalleById(Long id);
    List<Salle> getAllSalles();
}
