package com.onthi.v_edu.question.repository;

import com.onthi.v_edu.question.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface QuestionRepository extends JpaRepository<Question, Integer> {
    long countByTopic_IdAndDeletedAtIsNull(Integer topicId);

    Page<Question> findByTopic_IdAndDeletedAtIsNull(Integer topicId, Pageable pageable);

    Page<Question> findByTopic_Subject_IdAndDeletedAtIsNull(Integer subjectId, Pageable pageable);

    Page<Question> findByTopic_IdAndTopic_Subject_IdAndDeletedAtIsNull(Integer topicId, Integer subjectId, Pageable pageable);

    Page<Question> findByDeletedAtIsNull(Pageable pageable);

    java.util.Optional<Question> findByIdAndDeletedAtIsNull(Integer id);
}

