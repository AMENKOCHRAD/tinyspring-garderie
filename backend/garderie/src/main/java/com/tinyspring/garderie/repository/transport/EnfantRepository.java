package com.tinyspring.garderie.repository.transport;

import com.tinyspring.garderie.entity.transport.Enfant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnfantRepository extends JpaRepository<Enfant, Long> {
    Optional<Enfant> findByIdAndParentId(Long id, Long parentId);
    List<Enfant> findByParentId(Long parentId);
}
