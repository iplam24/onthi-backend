package com.onthi.v_edu.ai.service;

import com.onthi.v_edu.ai.entity.AiChatMessage;
import com.onthi.v_edu.ai.entity.AiChatSession;
import com.onthi.v_edu.ai.repository.AiChatMessageRepository;
import com.onthi.v_edu.ai.repository.AiChatSessionRepository;
import com.onthi.v_edu.common.ai.GitHubModelsClientService;
import com.onthi.v_edu.user.entity.User;
import com.onthi.v_edu.wallet.service.PlanService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiChatService {

    private static final Logger logger = LoggerFactory.getLogger(AiChatService.class);
    private final GitHubModelsClientService aiClientService;
    private final PlanService planService;
    private final AiChatMessageRepository aiChatMessageRepository;
    private final AiChatSessionRepository aiChatSessionRepository;

    public String chat(User user, String userMessage, Long sessionId) {
        // 1. Kiểm tra quyền truy cập tính năng Chatbot (Chỉ dành cho Pro và ProMax)
        if (!planService.canAccessFeature(user.getId(), "ai_chatbot")) {
            return "Hic, tính năng này chỉ dành cho gói Pro và ProMax thui ạ. Bạn nâng cấp để cùng mình học bài nhé! ✨";
        }

        logger.info("[AI CHAT] User {} is chatting with AI Study Buddy in session {}", user.getId(), sessionId);

        String systemPrompt = """
                Bạn là một Trợ lý học tập thông minh (AI Study Buddy) của nền tảng V-Edu.
                NHIỆM VỤ CỦA BẠN:
                - Giải đáp các thắc mắc về học tập, kiến thức phổ thông (Toán, Lý, Hóa, Văn, Anh...).
                - Cổ vũ, động viên tinh thần học tập của học sinh.
                - Trả lời ngắn gọn, súc tích, dễ hiểu và thân thiện.
                - Sử dụng icon/emoji phù hợp để cuộc hội thoại sinh động.
                """;

        String response = aiClientService.generateContent(userMessage, systemPrompt);

        // Lưu lịch sử nếu gói cước có quyền (ProMax)
        planService.getActiveUserPlan(user.getId()).ifPresent(up -> {
            if (up.getPlan().getHasAiHistory() != null && up.getPlan().getHasAiHistory()) {
                AiChatSession session = null;
                if (sessionId != null) {
                    session = aiChatSessionRepository.findById(sessionId).orElse(null);
                }

                // Nếu không có session và là ProMax, tự động tạo session mới với tiêu đề từ tin
                // nhắn đầu tiên
                if (session == null) {
                    session = createSession(user,
                            userMessage.length() > 30 ? userMessage.substring(0, 27) + "..." : userMessage);
                }

                saveMessage(user, userMessage, "user", session);
                saveMessage(user, response, "assistant", session);
            }
        });

        return response;
    }

    public AiChatSession createSession(User user, String title) {
        AiChatSession session = new AiChatSession();
        session.setUser(user);
        session.setTitle(title);
        session.setCreatedAt(LocalDateTime.now());
        return aiChatSessionRepository.save(session);
    }

    private void saveMessage(User user, String content, String role, AiChatSession session) {
        AiChatMessage message = new AiChatMessage();
        message.setUser(user);
        message.setSession(session);
        message.setContent(content);
        message.setRole(role);
        message.setCreatedAt(LocalDateTime.now());
        aiChatMessageRepository.save(message);
    }

    public List<AiChatSession> getUserSessions(User user) {
        return aiChatSessionRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    public List<AiChatMessage> getSessionMessages(Long sessionId) {
        return aiChatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    public List<AiChatMessage> getUserHistory(User user) {
        return aiChatMessageRepository.findByUserIdOrderByCreatedAtAsc(user.getId());
    }
}
