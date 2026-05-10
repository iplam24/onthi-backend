package com.onthi.v_edu.user.repository;

import com.onthi.v_edu.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface UserRepository extends JpaRepository<User, Integer> {
	Optional<User> findByUsername(String username);

	boolean existsByUsername(String username);

	boolean existsByEmail(String email);

	@Query("SELECT count(u) FROM User u WHERE u.createdAt >= :startOfDay")
	long countNewUsersSince(@Param("startOfDay") LocalDateTime startOfDay);

	@Query("SELECT u FROM User u LEFT JOIN UserInformation ui ON u.id = ui.user.id " +
			"WHERE u.username LIKE %:query% OR ui.fullName LIKE %:query%")
	List<User> searchUsers(@Param("query") String query);

	@Query("SELECT u FROM User u WHERE u.id IN :ids")
	Page<User> findByIdIn(@Param("ids") List<Integer> ids, Pageable pageable);
}
