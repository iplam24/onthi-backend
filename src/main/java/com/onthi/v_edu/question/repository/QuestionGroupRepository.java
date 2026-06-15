package com.onthi.v_edu.question.repository;

import com.onthi.v_edu.question.entity.QuestionGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionGroupRepository extends org.springframework.data.jpa.repository.JpaRepository<QuestionGroup, Integer> {
    org.springframework.data.domain.Page<QuestionGroup> findAll(org.springframework.data.domain.Pageable pageable);
    
    org.springframework.data.domain.Page<QuestionGroup> findByTopic_Subject_Id(Integer subjectId, org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<QuestionGroup> findByTopic_Id(Integer topicId, org.springframework.data.domain.Pageable pageable);
}
