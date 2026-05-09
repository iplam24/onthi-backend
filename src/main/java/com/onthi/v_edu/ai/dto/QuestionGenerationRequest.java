package com.onthi.v_edu.ai.dto;

import com.onthi.v_edu.common.constant.DifficultyLevel;
import com.onthi.v_edu.common.constant.QuestionType;
import lombok.Data;

@Data
public class QuestionGenerationRequest {
    private String content; // Văn bản gốc để tạo câu hỏi
    private Integer numberOfQuestions = 5;
    private QuestionType preferredType = QuestionType.MCQ;
    private DifficultyLevel difficulty = DifficultyLevel.MEDIUM;
    private String additionalInstructions;
}
