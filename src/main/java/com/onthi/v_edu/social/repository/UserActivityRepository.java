package com.onthi.v_edu.social.repository;

import com.onthi.v_edu.social.entity.UserActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {
    
    @Query("SELECT a FROM UserActivity a WHERE a.user.id IN :userIds ORDER BY a.createdAt DESC")
    Page<UserActivity> findByUserIds(List<Integer> userIds, Pageable pageable);
    
    Page<UserActivity> findByUserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);
}
