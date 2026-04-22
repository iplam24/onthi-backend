package com.onthi.v_edu.attempt.repository;

import com.onthi.v_edu.attempt.entity.Attempt;
import com.onthi.v_edu.common.constant.AttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AttemptRepository extends JpaRepository<Attempt, Integer> {

    Optional<Attempt> findByIdAndUser_Id(Integer id, Integer userId);

    boolean existsByUser_IdAndExam_IdAndStatus(Integer userId, Integer examId, AttemptStatus status);

    long countByUser_IdAndExam_Id(Integer userId, Integer examId);

    List<Attempt> findByUser_IdOrderByStartedAtDesc(Integer userId);
}

