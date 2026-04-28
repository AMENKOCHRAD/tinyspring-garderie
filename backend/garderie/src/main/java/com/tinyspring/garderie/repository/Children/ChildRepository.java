package com.tinyspring.garderie.repository.Children;

import com.tinyspring.garderie.entity.Children.Child;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ChildRepository extends JpaRepository<Child, Long> {

    List<Child> findByParentIdOrderByFirstNameAscLastNameAsc(Long parentId);

    List<Child> findByAllergiesIsNotNull();

    List<Child> findByClassroomIdIn(Collection<Long> classroomIds);
}