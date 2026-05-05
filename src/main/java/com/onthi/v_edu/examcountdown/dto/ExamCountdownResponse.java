package com.onthi.v_edu.examcountdown.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamCountdownResponse {
    private Integer id;
    private String title;
    private LocalDate examDate;
    private Integer levelId;
    private String levelName;
}
