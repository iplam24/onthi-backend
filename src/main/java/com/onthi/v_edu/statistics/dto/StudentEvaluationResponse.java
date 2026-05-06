package com.onthi.v_edu.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentEvaluationResponse {
    private Integer userId;
    private String username;
    private String fullName;
    private String schoolName;
    private Integer levelId;
    private String levelName;
    private LocalDate dob;
    private long totalAttempts;
    private long submittedAttempts;
    private long totalAnswers;
    private double averageScore;
    private double bestScore;
    private double latestScore;
    private double accuracyRate;
    private double averageDurationSeconds;
    private double knowledgeScore;
    private double speedScore;
    private double progressScore;
    private double disciplineScore;
    private double overallScore;
    private String performanceLabel;
    private String summary;
    private List<String> strengths;
    private List<String> weaknesses;
    private List<String> recommendations;
    private List<SubjectEvaluationResponse> subjectEvaluations;
    private List<TopicEvaluationResponse> topicEvaluations;
    private List<DifficultyEvaluationResponse> difficultyEvaluations;
}

