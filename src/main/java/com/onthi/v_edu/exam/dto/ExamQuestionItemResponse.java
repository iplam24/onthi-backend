package com.onthi.v_edu.exam.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExamQuestionItemResponse {

    private Integer questionId;

    private String questionContent;

    private String url; // Added field for image URL

    private Integer orderIndex;

    private Double score;

    private String contentSnapshot;

    private List<QuestionOptionResponse> options;
}
