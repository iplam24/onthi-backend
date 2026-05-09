package com.onthi.v_edu.ai.controller;

import com.onthi.v_edu.ai.service.AiRecommendationService;
import com.onthi.v_edu.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/ai-recommendation")
@RequiredArgsConstructor
public class AiRecommendationController {

    private final AiRecommendationService aiRecommendationService;

    @GetMapping("/my-path")
    public ApiResponse<Map<String, String>> getMyPath() {
        String path = aiRecommendationService.generatePersonalizedLearningPath();
        return new ApiResponse<>(HttpStatus.OK.value(), "Tạo lộ trình học tập thành công!", Map.of("learningPath", path != null ? path : "Không thể tạo lộ trình lúc này."));
    }
}
