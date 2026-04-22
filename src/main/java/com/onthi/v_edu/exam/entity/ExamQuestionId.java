package com.onthi.v_edu.exam.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ExamQuestionId implements Serializable {

    private Integer examId;
    private Integer questionId;
}