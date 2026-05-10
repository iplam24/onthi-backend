package com.onthi.v_edu.chat.controller;

import com.onthi.v_edu.chat.dto.ChatTyping;
import com.onthi.v_edu.chat.entity.ChatMessage;
import com.onthi.v_edu.chat.repository.ChatRepository;
import com.onthi.v_edu.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatRepository chatRepository;

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessage chatMessage) {
        chatMessage.setTimestamp(LocalDateTime.now());
        chatMessage.setRead(false);
        
        // Save to MongoDB
        ChatMessage saved = chatRepository.save(chatMessage);
        
        // Send to receiver via WebSocket
        String destination = "/queue/messages";
        log.info("📤 Sending message from {} to user {}. Destination: /user/{}{}", 
                chatMessage.getSenderId(), chatMessage.getReceiverId(), chatMessage.getReceiverId(), destination);
        
        messagingTemplate.convertAndSendToUser(
                String.valueOf(chatMessage.getReceiverId()), 
                destination, 
                saved
        );
    }

    @MessageMapping("/chat.typing")
    public void sendTyping(@Payload ChatTyping typing) {
        messagingTemplate.convertAndSendToUser(
                String.valueOf(typing.getReceiverId()),
                "/queue/typing",
                typing
        );
    }

    @GetMapping("/api/chat/history/{userId}/{contactId}")
    public ResponseEntity<ApiResponse<List<ChatMessage>>> getChatHistory(
            @PathVariable Integer userId,
            @PathVariable Integer contactId) {
        
        List<ChatMessage> history = chatRepository.findBySenderIdAndReceiverIdOrSenderIdAndReceiverIdOrderByTimestampAsc(
                userId, contactId, contactId, userId);
        
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Lấy lịch sử chat thành công", history));
    }

    @GetMapping("/api/chat/contacts/{userId}")
    public ResponseEntity<ApiResponse<List<Integer>>> getRecentContacts(@PathVariable Integer userId) {
        List<Integer> contacts = chatRepository.findDistinctContactIds(userId);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Lấy danh sách liên hệ thành công", contacts));
    }
}
