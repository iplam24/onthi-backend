package com.onthi.v_edu.ai.repository;

import com.onthi.v_edu.ai.entity.AiChatSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiChatSessionRepository extends JpaRepository<AiChatSession, Long> {
    Page<AiChatSession> findByUserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);
    Optional<AiChatSession> findFirstByUserIdOrderByCreatedAtDesc(Integer userId);
}
