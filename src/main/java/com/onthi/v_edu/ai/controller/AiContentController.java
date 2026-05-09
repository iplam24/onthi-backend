package com.onthi.v_edu.ai.controller;

import com.onthi.v_edu.ai.dto.GeneratedQuestionResponse;
import com.onthi.v_edu.ai.dto.QuestionGenerationRequest;
import com.onthi.v_edu.ai.service.AiContentService;
import com.onthi.v_edu.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai-content")
@RequiredArgsConstructor
public class AiContentController {

    private final AiContentService aiContentService;

    @PostMapping("/generate-questions")
    public ApiResponse<List<GeneratedQuestionResponse>> generateQuestions(@RequestBody QuestionGenerationRequest request) {
        List<GeneratedQuestionResponse> questions = aiContentService.generateQuestions(request);
        return new ApiResponse<>(HttpStatus.OK.value(), "Tạo câu hỏi thành công!", questions);
    }
}
