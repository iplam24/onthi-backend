package com.onthi.v_edu.exam.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RandomExamResponse {

    private Integer examId;
    private String title;
    private Integer subjectId;
    private String subjectName;
    private Integer duration;
    private Integer totalQuestions;

    /** Phân bổ theo mức độ khó: {"EASY": 10, "MEDIUM": 15, "HARD": 5} */
    private Map<String, Integer> difficultyDistribution;

    /** Phân bổ theo chủ đề: {"Đại số": 10, "Hình học": 15} */
    private Map<String, Integer> topicDistribution;

    private Boolean allowRetake;
    private Integer maxAttempts;
    private Boolean hasDuplicates;
    private LocalDateTime createdAt;
    
    /** Phân trăm trùng lặp với các đề cũ. Key = Exam ID, Value = % */
    private Map<Integer, Double> overlapPercentages;
}
