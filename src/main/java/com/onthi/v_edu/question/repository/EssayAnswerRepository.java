package com.onthi.v_edu.question.repository;

import com.onthi.v_edu.question.entity.EssayAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EssayAnswerRepository extends JpaRepository<EssayAnswer, Integer> {
	Optional<EssayAnswer> findByQuestion_IdAndDeletedAtIsNull(Integer questionId);

	@Modifying
	@Query("update EssayAnswer ea set ea.deletedAt = current_timestamp where ea.question.id = :questionId and ea.deletedAt is null")
	int softDeleteByQuestionId(@Param("questionId") Integer questionId);
}

