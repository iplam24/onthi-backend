package com.onthi.v_edu.question.repository;

import com.onthi.v_edu.question.entity.Explanation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ExplanationRepository extends JpaRepository<Explanation, Integer> {
	Optional<Explanation> findByQuestionIdAndDeletedAtIsNull(Integer questionId);

	@Modifying
	@Query("update Explanation e set e.deletedAt = current_timestamp where e.questionId = :questionId and e.deletedAt is null")
	int softDeleteByQuestionId(@Param("questionId") Integer questionId);
}

