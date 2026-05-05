package com.onthi.v_edu.examcountdown.repository;

import com.onthi.v_edu.examcountdown.entity.ExamCountdown;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamCountdownRepository extends JpaRepository<ExamCountdown, Integer> {
    List<ExamCountdown> findByLevel_IdOrderByExamDateAsc(Integer levelId);
}
