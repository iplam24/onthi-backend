package com.onthi.v_edu.exam.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExamQuestionItemRequest {

    @NotNull
    private Integer questionId;

    private Integer orderIndex;

    private Double score;

    private String contentSnapshot;
}

