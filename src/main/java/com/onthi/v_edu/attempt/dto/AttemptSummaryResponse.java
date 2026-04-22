package com.onthi.v_edu.attempt.dto;

import com.onthi.v_edu.common.constant.AttemptStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AttemptSummaryResponse {

    private Integer id;

    private Integer examId;

    private String examTitle;

    private AttemptStatus status;

    private Double score;

    private Integer correctCount;

    private Integer wrongCount;

    private Integer totalQuestions;

    private Integer durationTaken;

    private LocalDateTime startedAt;

    private LocalDateTime submittedAt;

    private LocalDateTime expiredAt;

    private Integer tabSwitchCount;

    private Integer violationScore;

    private Boolean flagged;
}

