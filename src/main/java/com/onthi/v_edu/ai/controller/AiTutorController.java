package com.onthi.v_edu.ai.controller;

import com.onthi.v_edu.ai.service.AiTutorService;
import com.onthi.v_edu.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/ai-tutor")
@RequiredArgsConstructor
public class AiTutorController {

    private final AiTutorService aiTutorService;

    @GetMapping("/explain")
    public ApiResponse<Map<String, String>> explain(@RequestParam Integer questionId, @RequestParam(required = false) String studentAnswer) {
        String explanation = aiTutorService.explainQuestion(questionId, studentAnswer);
        return new ApiResponse<>(HttpStatus.OK.value(), "Lấy giải thích thành công!", Map.of("explanation", explanation != null ? explanation : "Không thể tạo giải thích lúc này."));
    }
}
