package com.tinyspring.garderie.repository;

import com.tinyspring.garderie.entity.Role;
import com.tinyspring.garderie.entity.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}