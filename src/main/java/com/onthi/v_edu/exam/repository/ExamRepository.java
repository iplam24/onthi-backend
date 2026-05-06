package com.onthi.v_edu.exam.repository;

import com.onthi.v_edu.exam.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ExamRepository extends JpaRepository<Exam, Integer> {
    long countBySubject_IdAndDeletedAtIsNull(Integer subjectId);

    Page<Exam> findBySubject_IdAndDeletedAtIsNull(Integer subjectId, Pageable pageable);

    Page<Exam> findByDeletedAtIsNull(Pageable pageable);

    java.util.Optional<Exam> findByIdAndDeletedAtIsNull(Integer id);
}

