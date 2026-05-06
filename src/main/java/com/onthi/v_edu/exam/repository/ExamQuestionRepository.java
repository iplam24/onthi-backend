package com.onthi.v_edu.exam.repository;

import com.onthi.v_edu.exam.entity.ExamQuestion;
import com.onthi.v_edu.exam.entity.ExamQuestionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExamQuestionRepository extends JpaRepository<ExamQuestion, ExamQuestionId> {

    List<ExamQuestion> findByExam_IdAndDeletedAtIsNullOrderByOrderIndexAscQuestion_IdAsc(Integer examId);

    @Modifying
    @Query("update ExamQuestion eq set eq.deletedAt = current_timestamp where eq.exam.id = :examId and eq.deletedAt is null")
    int softDeleteByExamId(@Param("examId") Integer examId);

    @Modifying
    @Query("update ExamQuestion eq set eq.deletedAt = current_timestamp where eq.question.id = :questionId and eq.deletedAt is null")
    int softDeleteByQuestionId(@Param("questionId") Integer questionId);
}

