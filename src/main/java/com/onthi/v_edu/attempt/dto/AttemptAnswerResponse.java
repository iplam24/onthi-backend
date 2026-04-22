package com.onthi.v_edu.attempt.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AttemptAnswerResponse {

    private Integer questionId;

    private String questionContent;

    private Integer selectedOptionId;

    private String essayAnswer;

    private Boolean isCorrect;

    private Double score;
}

