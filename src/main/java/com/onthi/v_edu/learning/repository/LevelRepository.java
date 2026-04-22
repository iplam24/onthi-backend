package com.onthi.v_edu.learning.repository;

import com.onthi.v_edu.learning.entity.Level;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LevelRepository extends JpaRepository<Level, Integer> {
    Optional<Level> findByName(String name);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Integer id);
}

