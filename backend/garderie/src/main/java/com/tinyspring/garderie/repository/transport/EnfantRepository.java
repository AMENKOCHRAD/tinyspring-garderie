package com.tinyspring.garderie.repository.transport;

import com.tinyspring.garderie.entity.transport.Enfant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository("transportEnfantRepository")
public interface EnfantRepository extends JpaRepository<Enfant, Long> {
    Optional<Enfant> findByIdAndParentId(Long id, Long parentId);
    List<Enfant> findByParentId(Long parentId);
}
