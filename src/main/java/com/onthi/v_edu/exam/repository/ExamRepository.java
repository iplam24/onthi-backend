package com.onthi.v_edu.exam.repository;

import com.onthi.v_edu.exam.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExamRepository extends JpaRepository<Exam, Integer> {
    long countBySubject_Id(Integer subjectId);

    List<Exam> findBySubject_IdOrderByIdDesc(Integer subjectId);
}

