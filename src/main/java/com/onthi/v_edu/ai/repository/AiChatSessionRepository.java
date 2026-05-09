package com.onthi.v_edu.ai.repository;

import com.onthi.v_edu.ai.entity.AiChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AiChatSessionRepository extends JpaRepository<AiChatSession, Long> {
    List<AiChatSession> findByUserIdOrderByCreatedAtDesc(Integer userId);
}
