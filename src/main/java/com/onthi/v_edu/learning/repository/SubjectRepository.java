package com.onthi.v_edu.learning.repository;

import com.onthi.v_edu.learning.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Integer> {
    Optional<Subject> findByNameAndLevel_Id(String name, Integer levelId);

    boolean existsByNameAndLevel_Id(String name, Integer levelId);

    boolean existsByNameAndLevel_IdAndIdNot(String name, Integer levelId, Integer id);

    long countByLevel_Id(Integer levelId);
}

