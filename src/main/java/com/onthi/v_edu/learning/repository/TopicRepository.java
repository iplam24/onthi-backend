package com.onthi.v_edu.learning.repository;

import com.onthi.v_edu.learning.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TopicRepository extends JpaRepository<Topic, Integer> {
    Optional<Topic> findByNameAndSubject_Id(String name, Integer subjectId);

    boolean existsByNameAndSubject_Id(String name, Integer subjectId);

    boolean existsByNameAndSubject_IdAndIdNot(String name, Integer subjectId, Integer id);

    long countBySubject_Id(Integer subjectId);
}

