package com.tinyspring.garderie.repository.Classes;

import com.tinyspring.garderie.entity.Classes.Classe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("classesClasseRepository")
public interface ClasseRepository extends JpaRepository<Classe, Long> {
}
