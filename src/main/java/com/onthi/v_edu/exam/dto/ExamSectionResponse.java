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
public class ExamSectionResponse {

    private Integer sectionIndex;

    private String title;

    // MCQ / ESSAY / MIXED
    private String sectionType;

    private Integer questionCount;

    private Double totalScore;

    private Integer startOrderIndex;

    private Integer endOrderIndex;

    private List<ExamQuestionItemResponse> questions;
}

