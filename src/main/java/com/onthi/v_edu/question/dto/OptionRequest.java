package com.onthi.v_edu.question.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OptionRequest {
	@NotBlank(message = "Nội dung đáp án không được để trống")
	private String content;

	@NotNull(message = "Vui lòng chỉ định đáp án này là đúng hay sai")
	private Boolean isCorrect;

	private String imageUrl;
}

