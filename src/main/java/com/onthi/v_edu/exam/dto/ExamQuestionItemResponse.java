package com.onthi.v_edu.exam.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExamQuestionItemResponse {

    private Integer questionId;

    private String questionContent;

    private Integer orderIndex;

    private Double score;

    private String contentSnapshot;
}

