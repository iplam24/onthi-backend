package com.onthi.v_edu.attempt.repository;

import com.onthi.v_edu.attempt.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Integer> {

    @Query("SELECT a FROM Answer a " +
           "LEFT JOIN FETCH a.question " +
           "LEFT JOIN FETCH a.selectedOption " +
           "WHERE a.attempt.id = :attemptId " +
           "ORDER BY a.id ASC")
    List<Answer> findByAttemptIdWithDetails(Integer attemptId);

    List<Answer> findByAttempt_IdOrderByIdAsc(Integer attemptId);

    @Modifying
    void deleteByAttempt_Id(Integer attemptId);
}
