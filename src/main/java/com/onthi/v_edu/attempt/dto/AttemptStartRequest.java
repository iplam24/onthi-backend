package com.onthi.v_edu.attempt.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AttemptStartRequest {

    @NotNull
    private Integer examId;
}

