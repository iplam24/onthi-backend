package com.onthi.v_edu.attempt.dto;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AttemptSubmitRequest {

    private List<@Valid AttemptSubmitAnswerRequest> answers;

    private Integer tabSwitchCount;

    private Integer violationScore;
}

