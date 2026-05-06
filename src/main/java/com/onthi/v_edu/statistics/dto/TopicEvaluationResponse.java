package com.onthi.v_edu.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopicEvaluationResponse {
    private Integer topicId;
    private String topicName;
    private Integer subjectId;
    private String subjectName;
    private long totalAnswers;
    private long correctAnswers;
    private double accuracyRate;
    private double averageScore;
}

