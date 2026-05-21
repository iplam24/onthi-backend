package com.onthi.v_edu.exam.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserExamHistoryResponse {

    private Integer examId;
    private String examTitle;
    private Integer subjectId;
    private String subjectName;
    private Integer attemptCount;
    private Integer maxAttempts;
    private Boolean canRetake;
    private Double bestScore;
    private Double latestScore;
    private LocalDateTime lastAttemptAt;
    private String examType;            // AUTO / MANUAL
}
