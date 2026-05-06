package com.onthi.v_edu.attempt.dto;

import com.onthi.v_edu.common.constant.AttemptStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AttemptFilterRequest {

    private Integer subjectId;

    private Integer levelId;

    private Integer examId;

    private AttemptStatus status;

    private Boolean flagged;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime from;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime to;

    private String keyword;
}

