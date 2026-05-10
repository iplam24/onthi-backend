package com.onthi.v_edu.ai.controller;

import com.onthi.v_edu.ai.dto.AiChatMessageRequest;
import com.onthi.v_edu.ai.dto.AiChatResponse;
import com.onthi.v_edu.ai.entity.AiChatMessage;
import com.onthi.v_edu.ai.entity.AiChatSession;
import com.onthi.v_edu.ai.service.AiChatService;
import com.onthi.v_edu.common.dto.ApiResponse;
import com.onthi.v_edu.config.security.services.UserDetailsImpl;
import com.onthi.v_edu.user.entity.User;
import com.onthi.v_edu.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai/chat")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;
    private final UserService userService;

    @PostMapping("/send")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AiChatResponse>> chat(@RequestBody AiChatMessageRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        try {
            AiChatResponse response = aiChatService.chat(user, request.getMessage(), request.getSessionId());
            return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Phản hồi từ AI", response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(HttpStatus.FORBIDDEN.value(), e.getMessage(), null));
        }
    }

    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<AiChatMessage>>> getChatHistory(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<AiChatMessage> history = aiChatService.getUserHistory(user);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Lịch sử trò chuyện", history));
    }

    @GetMapping("/sessions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<AiChatSession>>> getSessions(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Danh sách phiên chat", aiChatService.getUserSessions(user, pageable)));
    }

    @GetMapping("/sessions/{id}/messages")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<AiChatMessage>>> getSessionMessages(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Tin nhắn trong phiên", aiChatService.getSessionMessages(id)));
    }

    @DeleteMapping("/sessions/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteSession(@PathVariable Long id) {
        aiChatService.deleteSession(id);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Đã xóa phiên chat", null));
    }
}
