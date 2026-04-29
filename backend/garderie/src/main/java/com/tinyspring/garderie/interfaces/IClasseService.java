package com.tinyspring.garderie.interfaces;

import com.tinyspring.garderie.entity.Classe;
import java.util.List;

public interface IClasseService {
    Classe addClasse(Classe classe);
    Classe updateClasse(Classe classe);
    void deleteClasse(Long id);
    Classe getClasseById(Long id);
    List<Classe> getAllClasses();
}
