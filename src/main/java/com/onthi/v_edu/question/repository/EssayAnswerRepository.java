package com.onthi.v_edu.question.repository;

import com.onthi.v_edu.question.entity.EssayAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EssayAnswerRepository extends JpaRepository<EssayAnswer, Integer> {
	Optional<EssayAnswer> findByQuestion_Id(Integer questionId);

	void deleteByQuestion_Id(Integer questionId);
}

