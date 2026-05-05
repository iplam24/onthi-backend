package com.onthi.v_edu.user.repository;

import com.onthi.v_edu.user.entity.UserStudyStreak;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserStudyStreakRepository extends JpaRepository<UserStudyStreak, Integer> {
	Optional<UserStudyStreak> findByUser_Id(Integer userId);
}

