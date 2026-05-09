package com.onthi.v_edu.ai.entity;

import com.onthi.v_edu.user.entity.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_chat_messages")
@Getter
@Setter
public class AiChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private AiChatSession session;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String role; // user / assistant

    private LocalDateTime createdAt;
}
