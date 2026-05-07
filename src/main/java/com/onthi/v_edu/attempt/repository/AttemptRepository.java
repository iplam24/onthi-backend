package com.onthi.v_edu.attempt.repository;

import com.onthi.v_edu.attempt.entity.Attempt;
import com.onthi.v_edu.common.constant.AttemptStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface AttemptRepository extends JpaRepository<Attempt, Integer> {

    Optional<Attempt> findByIdAndUser_Id(Integer id, Integer userId);

    boolean existsByUser_IdAndExam_IdAndStatus(Integer userId, Integer examId, AttemptStatus status);

    @EntityGraph(attributePaths = {"exam", "exam.subject", "exam.subject.level"})
    List<Attempt> findByStatus(AttemptStatus status);

    long countByUser_IdAndExam_Id(Integer userId, Integer examId);

    List<Attempt> findByUser_IdOrderByStartedAtDesc(Integer userId);

    Page<Attempt> findByUser_Id(Integer userId, Pageable pageable);

    @EntityGraph(attributePaths = {"exam", "exam.subject", "exam.subject.level"})
    @Query("""
            select distinct a
            from Attempt a
            join a.exam e
            left join e.subject s
            left join s.level l
            where a.user.id = :userId
              and (:subjectId is null or s.id = :subjectId)
              and (:levelId is null or l.id = :levelId)
              and (:examId is null or e.id = :examId)
              and (:status is null or a.status = :status)
              and (:flagged is null or a.flagged = :flagged)
              and (:from is null or a.startedAt >= :from)
              and (:to is null or a.startedAt <= :to)
              and (:keyword is null or lower(e.title) like lower(concat('%', :keyword, '%')))
            """)
    Page<Attempt> searchMyAttempts(@Param("userId") Integer userId,
                                   @Param("subjectId") Integer subjectId,
                                   @Param("levelId") Integer levelId,
                                   @Param("examId") Integer examId,
                                   @Param("status") AttemptStatus status,
                                   @Param("flagged") Boolean flagged,
                                   @Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to,
                                   @Param("keyword") String keyword,
                                   Pageable pageable);
}
