package com.onthi.v_edu.exam.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class ExamRequest {

    @NotBlank
    private String title;

    @NotNull
    private Integer subjectId;

    @NotNull
    private Integer duration;

    private Boolean isActive;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Double totalScore;

    private String type;

    private Boolean shuffleQuestions;

    private Boolean shuffleAnswers;

    private Integer maxAttempts;

    private List<@Valid ExamQuestionItemRequest> questions;
}

