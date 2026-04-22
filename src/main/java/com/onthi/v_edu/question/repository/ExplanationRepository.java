package com.onthi.v_edu.question.repository;

import com.onthi.v_edu.question.entity.Explanation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExplanationRepository extends JpaRepository<Explanation, Integer> {
	Optional<Explanation> findByQuestionId(Integer questionId);

	void deleteByQuestionId(Integer questionId);
}

