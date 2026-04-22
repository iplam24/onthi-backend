package com.onthi.v_edu.question.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OptionResponse {
	private Integer id;
	private String content;
	private Boolean isCorrect;
}

