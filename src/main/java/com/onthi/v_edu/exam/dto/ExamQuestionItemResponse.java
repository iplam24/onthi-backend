package com.onthi.v_edu.exam.dto;

import com.onthi.v_edu.common.constant.ContentFormat;
import com.onthi.v_edu.common.constant.QuestionType;
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

    private ContentFormat questionContentFormat;

    private QuestionType questionType;

    private String url; // image URL

    private String audioUrl;

    private Integer orderIndex;

    private Double score;

    private String sectionName;

    private String contentSnapshot;

    private ContentFormat contentFormatSnapshot;

    private Integer questionGroupId;

    private String questionGroupTitle;

    private String questionGroupContent;

    private List<QuestionOptionResponse> options;
}
