package com.onthi.v_edu.exam.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ExamSectionRequest {
    
    @NotBlank(message = "Tên phần không được để trống")
    private String sectionName;
    
    private List<@Valid ExamQuestionItemRequest> items;
}
