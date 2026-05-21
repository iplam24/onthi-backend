package com.onthi.v_edu.exam.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExamResponse {

    private Integer id;

    private String title;

    private Integer subjectId;

    private String subjectName;

    private Integer createdById;

    private String createdByUsername;

    private Integer duration;

    private Boolean isActive;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Double totalScore;

    private String type;

    private Boolean isPublic;

    // UI hint for frontend rendering: STANDARD, LITERATURE, ESSAY, MIXED
    private String uiLayoutHint;

    private List<ExamSectionResponse> sections;

    private Boolean shuffleQuestions;

    private Boolean shuffleAnswers;

    private Integer maxAttempts;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<ExamQuestionItemResponse> questions;
}

