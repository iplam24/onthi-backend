package com.onthi.v_edu.progress.repository;

import com.onthi.v_edu.progress.entity.Progress;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgressRepository extends JpaRepository<Progress, Integer> {
    long countByTopic_Id(Integer topicId);
}

