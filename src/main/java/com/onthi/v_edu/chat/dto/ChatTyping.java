package com.onthi.v_edu.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatTyping {
    private Integer senderId;
    private Integer receiverId;
    private boolean isTyping;
}
