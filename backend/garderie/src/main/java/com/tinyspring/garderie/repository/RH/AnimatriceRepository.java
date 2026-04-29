package com.tinyspring.garderie.repository.RH;

import com.tinyspring.garderie.entity.RH.Animatrice;
import com.tinyspring.garderie.entity.RH.enums.StatutAnimatrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnimatriceRepository extends JpaRepository<Animatrice, Long> {

    List<Animatrice> findByStatut(StatutAnimatrice statut);

    Optional<Animatrice> findByEmail(String email);

    List<Animatrice> findBySpecialite(String specialite);

    boolean existsByEmail(String email);
}