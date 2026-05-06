package com.onthi.v_edu.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DifficultyEvaluationResponse {
    private String difficulty;
    private long totalAnswers;
    private long correctAnswers;
    private double accuracyRate;
    private double averageScore;
}

