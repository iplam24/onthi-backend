package com.onthi.v_edu.ai.dto;

import com.onthi.v_edu.common.constant.DifficultyLevel;
import com.onthi.v_edu.common.constant.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedQuestionResponse {
    private String content;
    private QuestionType type;
    private DifficultyLevel difficulty;
    private List<OptionDto> options;
    private String explanation;
    private String sampleAnswer; // Cho tự luận

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionDto {
        private String content;
        private Boolean isCorrect;
    }
}
