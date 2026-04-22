package com.onthi.v_edu.question.repository;

import com.onthi.v_edu.question.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, Integer> {
    long countByTopic_Id(Integer topicId);
}

