package com.onthi.v_edu.exam.repository;

import com.onthi.v_edu.exam.entity.ExamQuestion;
import com.onthi.v_edu.exam.entity.ExamQuestionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExamQuestionRepository extends JpaRepository<ExamQuestion, ExamQuestionId> {

    List<ExamQuestion> findByExam_IdOrderByOrderIndexAscQuestion_IdAsc(Integer examId);

    void deleteByExam_Id(Integer examId);
}

