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

    @org.springframework.data.jpa.repository.Query("SELECT e FROM Exam e WHERE e.deletedAt IS NULL AND " +
            "(e.isPublic = true OR (e.isPublic IS NULL AND e.type = 'MANUAL') OR (e.createdBy IS NOT NULL AND e.createdBy.id = :userId))")
    Page<Exam> findVisibleExams(@org.springframework.data.repository.query.Param("userId") Integer userId, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT e FROM Exam e WHERE e.deletedAt IS NULL AND e.subject.id = :subjectId AND " +
            "(e.isPublic = true OR (e.isPublic IS NULL AND e.type = 'MANUAL') OR (e.createdBy IS NOT NULL AND e.createdBy.id = :userId))")
    Page<Exam> findVisibleExamsBySubject(@org.springframework.data.repository.query.Param("subjectId") Integer subjectId, @org.springframework.data.repository.query.Param("userId") Integer userId, Pageable pageable);

    boolean existsByCreatedBy_IdAndExamHashAndDeletedAtIsNull(Integer userId, String examHash);

    @org.springframework.data.jpa.repository.Query("SELECT e FROM Exam e WHERE e.deletedAt IS NULL AND e.createdBy.id = :userId AND e.subject.id = :subjectId AND e.type = 'AUTO' ORDER BY e.createdAt DESC")
    java.util.List<Exam> findRecentAutoExamsByUserAndSubject(
            @org.springframework.data.repository.query.Param("userId") Integer userId,
            @org.springframework.data.repository.query.Param("subjectId") Integer subjectId,
            Pageable pageable);
}

