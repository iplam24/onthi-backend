package com.onthi.v_edu.exam.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExamPerformanceResponse {

    // --- Tổng quan ---
    private Integer attemptId;
    private Integer examId;
    private String examTitle;
    private Double score;
    private Double percentage;
    private String overallRating;      // EXCELLENT, GOOD, AVERAGE, WEAK, VERY_WEAK
    private Integer correctCount;
    private Integer wrongCount;
    private Integer unansweredCount;
    private Integer totalQuestions;
    private Integer durationTaken;

    // --- Phân tích theo chủ đề ---
    private List<TopicAnalysis> topicAnalyses;

    // --- Phân tích theo mức độ khó ---
    private List<DifficultyAnalysis> difficultyAnalyses;

    // --- Điểm yếu & gợi ý ---
    private List<WeaknessItem> weaknesses;
    private List<String> recommendations;

    // --- So sánh với lần trước ---
    private ProgressComparison progressComparison;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopicAnalysis {
        private Integer topicId;
        private String topicName;
        private Integer totalQuestions;
        private Integer correctCount;
        private Double percentage;
        private String rating;         // STRONG, AVERAGE, WEAK
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DifficultyAnalysis {
        private String difficulty;     // EASY, MEDIUM, HARD
        private Integer totalQuestions;
        private Integer correctCount;
        private Double percentage;
        private String rating;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeaknessItem {
        private String area;           // Tên topic hoặc difficulty
        private String type;           // TOPIC hoặc DIFFICULTY
        private Double percentage;
        private String description;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgressComparison {
        private Double previousScore;
        private Double currentScore;
        private Double improvement;    // % cải thiện (positive = improving)
        private String trend;          // IMPROVING, STABLE, DECLINING
        private Integer attemptNumber;
    }
}
