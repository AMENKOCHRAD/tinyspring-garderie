package com.tinyspring.garderie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tinyspring.garderie.entity.Groupe;

@Repository
public interface GroupeRepository extends JpaRepository<Groupe, Long> {
}
