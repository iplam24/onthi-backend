package com.onthi.v_edu.question.dto;

import com.onthi.v_edu.common.constant.ContentFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class QuestionGroupRequest {
    private String title;
    
    @NotBlank(message = "Nội dung nhóm câu hỏi không được để trống")
    private String content;

    private ContentFormat contentFormat;
    private String audioUrl;

    @NotNull(message = "Vui lòng chọn chủ đề")
    private Integer topicId;

    @Valid
    private List<QuestionRequest> questions;
}
