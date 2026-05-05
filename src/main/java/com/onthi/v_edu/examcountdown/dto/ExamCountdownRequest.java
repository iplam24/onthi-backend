package com.onthi.v_edu.examcountdown.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamCountdownRequest {
    @NotBlank
    private String title;

    @NotNull
    private LocalDate examDate;

    @NotNull
    private Integer levelId;
}
