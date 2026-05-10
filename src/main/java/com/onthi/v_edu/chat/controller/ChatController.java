package com.onthi.v_edu.chat.controller;

import com.onthi.v_edu.chat.entity.ChatMessage;
import com.onthi.v_edu.chat.repository.ChatRepository;
import com.onthi.v_edu.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequiredArgsConstructor
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
        // Destination: /user/{receiverId}/queue/messages
        messagingTemplate.convertAndSendToUser(
                String.valueOf(chatMessage.getReceiverId()), 
                "/queue/messages", 
                saved
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
