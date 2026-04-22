package com.onthi.v_edu.userquestion.repository;

import com.onthi.v_edu.userquestion.entity.UserQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserQuestionRepository extends JpaRepository<UserQuestion, Integer> {
    long countByTopic_Id(Integer topicId);
}

