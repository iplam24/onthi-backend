package com.onthi.v_edu.ai.dto;

import lombok.Data;

@Data
public class AiChatMessageRequest {
    private String message;
    private Long sessionId;
}
