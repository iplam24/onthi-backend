package com.onthi.v_edu.question.repository;

import com.onthi.v_edu.question.entity.QuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuestionOptionRepository extends JpaRepository<QuestionOption, Integer> {
	List<QuestionOption> findByQuestion_IdAndDeletedAtIsNullOrderByIdAsc(Integer questionId);

	Optional<QuestionOption> findByIdAndQuestion_IdAndDeletedAtIsNull(Integer id, Integer questionId);

	Optional<QuestionOption> findFirstByQuestion_IdAndIsCorrectTrueAndDeletedAtIsNull(Integer questionId);

	@Modifying
	@Query("update QuestionOption qo set qo.deletedAt = current_timestamp where qo.question.id = :questionId and qo.deletedAt is null")
	int softDeleteByQuestionId(@Param("questionId") Integer questionId);
}

