package com.onthi.v_edu.social.repository;

import com.onthi.v_edu.social.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    Page<Notification> findByUser_IdOrderByCreatedAtDesc(Integer userId, Pageable pageable);
    long countByUser_IdAndReadFalse(Integer userId);
}
