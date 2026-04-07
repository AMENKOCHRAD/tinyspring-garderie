package com.tinyspring.garderie.repository;

import com.tinyspring.garderie.entity.Enfant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnfantRepository extends JpaRepository<Enfant, Long> {

    // Récupérer les enfants directement par l'ID du parent
    List<Enfant> findByParentId(Long parentId);
}