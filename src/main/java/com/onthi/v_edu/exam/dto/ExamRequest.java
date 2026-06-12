package com.onthi.v_edu.exam.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class ExamRequest {

    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;

    @NotNull(message = "Vui lòng chọn môn học")
    private Integer subjectId;

    @NotNull(message = "Vui lòng nhập thời gian làm bài")
    @Min(value = 1, message = "Thời gian làm bài tối thiểu là 1 phút")
    private Integer duration;

    private Boolean isActive;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @Positive(message = "Tổng điểm phải lớn hơn 0")
    private Double totalScore;

    private String type;

    private Boolean isPublic;

    private String uiLayoutHint;

    private Boolean shuffleQuestions;

    private Boolean shuffleAnswers;

    @Min(value = 0, message = "Số lần làm bài tối đa không được âm")
    private Integer maxAttempts;

    private List<@Valid ExamQuestionItemRequest> questions;
}