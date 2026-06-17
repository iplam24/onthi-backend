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
	private Integer id;

	@NotBlank(message = "Nội dung câu hỏi không được để trống")
	private String content;

	private ContentFormat contentFormat;

	private String url;

	private String audioUrl;

	@NotNull(message = "Vui lòng chọn loại câu hỏi")
	private QuestionType type;

	@NotNull(message = "Vui lòng chọn mức độ khó")
	private DifficultyLevel difficulty;

	@NotNull(message = "Vui lòng chọn chủ đề")
	private Integer topicId;

	private List<@Valid OptionRequest> options;

	private String sampleAnswer;

	private String explanation;
}

