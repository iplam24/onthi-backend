package com.onthi.v_edu.user.repository;

import com.onthi.v_edu.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
	Optional<User> findByUsername(String username);

	boolean existsByUsername(String username);

	boolean existsByEmail(String email);

	@Query("SELECT count(u) FROM User u WHERE u.createdAt >= :startOfDay")
	long countNewUsersSince(@Param("startOfDay") LocalDateTime startOfDay);
}
