package com.onthi.v_edu.question.dto;

import com.onthi.v_edu.common.constant.ContentFormat;
import com.onthi.v_edu.common.constant.DifficultyLevel;
import com.onthi.v_edu.common.constant.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class QuestionRequest {
	@NotBlank
	private String content;

	private ContentFormat contentFormat;

	private String url;

	@NotNull
	private QuestionType type;

	@NotNull
	private DifficultyLevel difficulty;

	@NotNull
	private Integer topicId;

	private List<@Valid OptionRequest> options;

	private String sampleAnswer;

	private String explanation;
}

