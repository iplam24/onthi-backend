package com.onthi.v_edu.attempt.repository;

import com.onthi.v_edu.attempt.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnswerRepository extends JpaRepository<Answer, Integer> {

    List<Answer> findByAttempt_IdOrderByIdAsc(Integer attemptId);

    void deleteByAttempt_Id(Integer attemptId);
}

