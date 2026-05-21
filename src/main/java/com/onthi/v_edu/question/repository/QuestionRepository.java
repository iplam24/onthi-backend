package com.onthi.v_edu.question.repository;

import com.onthi.v_edu.common.constant.DifficultyLevel;
import com.onthi.v_edu.question.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Integer> {
    long countByTopic_IdAndDeletedAtIsNull(Integer topicId);

    Page<Question> findByTopic_IdAndDeletedAtIsNull(Integer topicId, Pageable pageable);

    Page<Question> findByTopic_Subject_IdAndDeletedAtIsNull(Integer subjectId, Pageable pageable);

    Page<Question> findByTopic_IdAndTopic_Subject_IdAndDeletedAtIsNull(Integer topicId, Integer subjectId, Pageable pageable);

    Page<Question> findByDeletedAtIsNull(Pageable pageable);

    java.util.Optional<Question> findByIdAndDeletedAtIsNull(Integer id);

    // --- Random exam generation queries ---

    @Query(value = """
            SELECT q FROM Question q
            WHERE q.topic.id = :topicId
              AND q.difficulty = :difficulty
              AND q.deletedAt IS NULL
              AND q.id NOT IN :excludeIds
            ORDER BY FUNCTION('RAND')
            """)
    List<Question> findRandomByTopicAndDifficulty(
            @Param("topicId") Integer topicId,
            @Param("difficulty") DifficultyLevel difficulty,
            @Param("excludeIds") List<Integer> excludeIds,
            Pageable pageable);

    @Query(value = """
            SELECT q FROM Question q
            WHERE q.topic.subject.id = :subjectId
              AND q.difficulty = :difficulty
              AND q.deletedAt IS NULL
              AND q.id NOT IN :excludeIds
            ORDER BY FUNCTION('RAND')
            """)
    List<Question> findRandomBySubjectAndDifficulty(
            @Param("subjectId") Integer subjectId,
            @Param("difficulty") DifficultyLevel difficulty,
            @Param("excludeIds") List<Integer> excludeIds,
            Pageable pageable);

    @Query("""
            SELECT COUNT(q) FROM Question q
            WHERE q.topic.id = :topicId
              AND q.difficulty = :difficulty
              AND q.deletedAt IS NULL
              AND q.id NOT IN :excludeIds
            """)
    long countAvailableByTopicAndDifficulty(
            @Param("topicId") Integer topicId,
            @Param("difficulty") DifficultyLevel difficulty,
            @Param("excludeIds") List<Integer> excludeIds);

    @Query("""
            SELECT COUNT(q) FROM Question q
            WHERE q.topic.subject.id = :subjectId
              AND q.difficulty = :difficulty
              AND q.deletedAt IS NULL
              AND q.id NOT IN :excludeIds
            """)
    long countAvailableBySubjectAndDifficulty(
            @Param("subjectId") Integer subjectId,
            @Param("difficulty") DifficultyLevel difficulty,
            @Param("excludeIds") List<Integer> excludeIds);

    @Query(value = """
            SELECT q FROM Question q
            WHERE q.topic.id = :topicId
              AND q.deletedAt IS NULL
              AND q.id NOT IN :excludeIds
            ORDER BY FUNCTION('RAND')
            """)
    List<Question> findRandomByTopic(
            @Param("topicId") Integer topicId,
            @Param("excludeIds") List<Integer> excludeIds,
            Pageable pageable);

    @Query(value = """
            SELECT q FROM Question q
            WHERE q.topic.subject.id = :subjectId
              AND q.deletedAt IS NULL
              AND q.id NOT IN :excludeIds
            ORDER BY FUNCTION('RAND')
            """)
    List<Question> findRandomBySubject(
            @Param("subjectId") Integer subjectId,
            @Param("excludeIds") List<Integer> excludeIds,
            Pageable pageable);
}

