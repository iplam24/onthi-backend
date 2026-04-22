package com.onthi.v_edu.question.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OptionRequest {
	@NotBlank
	private String content;

	@NotNull
	private Boolean isCorrect;
}

