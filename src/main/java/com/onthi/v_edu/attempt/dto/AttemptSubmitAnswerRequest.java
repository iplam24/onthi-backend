package com.onthi.v_edu.attempt.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AttemptSubmitAnswerRequest {

    @NotNull
    private Integer questionId;

    private Integer selectedOptionId;

    private String essayAnswer;
}

