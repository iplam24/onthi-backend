package com.onthi.v_edu.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.onthi.v_edu.ai.dto.GeneratedQuestionResponse;
import com.onthi.v_edu.ai.dto.QuestionGenerationRequest;
import com.onthi.v_edu.common.ai.GitHubModelsClientService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiContentService {

    private static final Logger logger = LoggerFactory.getLogger(AiContentService.class);
    private final GitHubModelsClientService aiClientService;

    public List<GeneratedQuestionResponse> generateQuestions(QuestionGenerationRequest request) {
        String prompt = buildGenerationPrompt(request);
        logger.info("[AI CONTENT] Generating questions using GitHub Models...");

        String responseText = aiClientService.generateContent(prompt, "Bạn là chuyên gia soạn đề thi. Chỉ trả về JSON.");
        if (responseText == null) {
            return new ArrayList<>();
        }

        return parseGeneratedQuestions(responseText);
    }

    private String buildGenerationPrompt(QuestionGenerationRequest request) {
        return String.format("""
                Bạn là một chuyên gia soạn đề thi chuyên nghiệp.
                
                NHIỆM VỤ:
                Hãy tạo ra %d câu hỏi dựa trên nội dung văn bản dưới đây.
                
                YÊU CẦU:
                - Loại câu hỏi: %s
                - Độ khó: %s
                - Ngôn ngữ: Tiếng Việt (hoặc theo ngôn ngữ của văn bản gốc).
                - %s
                
                ĐỊNH DẠNG ĐẦU RA (JSON):
                Trả về một mảng JSON các đối tượng. 
                QUAN TRỌNG: Các ký tự gạch chéo ngược (\\) trong công thức Toán học (LaTeX) PHẢI được escape đúng chuẩn JSON (ví dụ viết là \\\\( hoặc \\\\[ ).
                
                Cấu trúc:
                {
                  "content": "Nội dung câu hỏi",
                  "type": "%s",
                  "difficulty": "%s",
                  "options": [ // Chỉ dành cho MCQ, để trống nếu là ESSAY
                    {"content": "Lựa chọn A", "isCorrect": false},
                    {"content": "Lựa chọn B", "isCorrect": true}
                  ],
                  "explanation": "Giải thích chi tiết tại sao chọn đáp án này",
                  "sampleAnswer": "Đáp án mẫu nếu là ESSAY"
                }
                
                VĂN BẢN GỐC:
                %s
                """,
                request.getNumberOfQuestions(),
                request.getPreferredType(),
                request.getDifficulty(),
                request.getAdditionalInstructions() != null ? request.getAdditionalInstructions() : "",
                request.getPreferredType(),
                request.getDifficulty(),
                request.getContent()
        );
    }

    private List<GeneratedQuestionResponse> parseGeneratedQuestions(String responseText) {
        try {
            String cleaned = responseText.replaceAll("```json", "").replaceAll("```", "").trim();
            
            // Regex thần thánh: Tìm tất cả các dấu \ mà theo sau KHÔNG phải là các ký tự escape JSON chuẩn
            // (n, r, t, b, f, u, ", \, /). Nếu tìm thấy, ta sẽ biến \ thành \\ để Jackson không báo lỗi.
            String fixedJson = cleaned.replaceAll("\\\\(?![nrtbfu\"\\\\/])", "\\\\\\\\");

            return aiClientService.getObjectMapper().readValue(fixedJson, new TypeReference<List<GeneratedQuestionResponse>>() {});
        } catch (Exception e) {
            logger.error("[AI CONTENT] Error parsing generated questions: {}", e.getMessage());
            logger.debug("[AI CONTENT] Raw response was: {}", responseText);
            return new ArrayList<>();
        }
    }
}
