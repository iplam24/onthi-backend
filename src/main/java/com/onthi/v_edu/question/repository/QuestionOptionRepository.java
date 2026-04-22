package com.onthi.v_edu.question.repository;

import com.onthi.v_edu.question.entity.QuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuestionOptionRepository extends JpaRepository<QuestionOption, Integer> {
	List<QuestionOption> findByQuestion_IdOrderByIdAsc(Integer questionId);

	Optional<QuestionOption> findByIdAndQuestion_Id(Integer id, Integer questionId);

	Optional<QuestionOption> findFirstByQuestion_IdAndIsCorrectTrue(Integer questionId);

	void deleteByQuestion_Id(Integer questionId);
}

