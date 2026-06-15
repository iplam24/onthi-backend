package com.onthi.v_edu.exam.dto;

import com.onthi.v_edu.common.constant.ContentFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExamQuestionItemRequest {

    private Integer questionId;

    private Integer groupId;

    private Integer orderIndex;

    private Double score;

    private String contentSnapshot;

    private ContentFormat contentFormatSnapshot;
}

