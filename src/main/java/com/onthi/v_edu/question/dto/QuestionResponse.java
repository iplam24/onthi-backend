package com.onthi.v_edu.question.dto;

import com.onthi.v_edu.common.constant.DifficultyLevel;
import com.onthi.v_edu.common.constant.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResponse {
	private Integer id;
	private String content;
	private QuestionType type;
	private DifficultyLevel difficulty;
	private Integer topicId;
	private String topicName;
	private Integer createdById;
	private String createdByUsername;
	private LocalDateTime createdAt;
	private List<OptionResponse> options;
	private String sampleAnswer;
	private String explanation;
	private LocalDateTime explanationCreatedAt;
}

