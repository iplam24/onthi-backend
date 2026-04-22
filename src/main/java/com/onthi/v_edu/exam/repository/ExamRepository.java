package com.onthi.v_edu.exam.repository;

import com.onthi.v_edu.exam.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ExamRepository extends JpaRepository<Exam, Integer> {
    long countBySubject_Id(Integer subjectId);

    Page<Exam> findBySubject_Id(Integer subjectId, Pageable pageable);
}

