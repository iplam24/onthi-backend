package com.onthi.v_edu.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubjectEvaluationResponse {
    private Integer subjectId;
    private String subjectName;
    private Integer levelId;
    private String levelName;
    private long attemptCount;
    private double averageScore;
    private double accuracyRate;
    private double bestScore;
    private double latestScore;
    private double averageDurationSeconds;
}

