package com.tinyspring.garderie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tinyspring.garderie.entity.Classe;

@Repository("mainClasseRepository")
public interface ClasseRepository extends JpaRepository<Classe, Long> {
}
