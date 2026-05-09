package com.onthi.v_edu.ai.repository;

import com.onthi.v_edu.ai.entity.AiChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AiChatMessageRepository extends JpaRepository<AiChatMessage, Long> {
    List<AiChatMessage> findByUserIdOrderByCreatedAtAsc(Integer userId);
    List<AiChatMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);
}
