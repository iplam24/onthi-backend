package com.onthi.v_edu.attempt.dto;

import com.onthi.v_edu.common.constant.ContentFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AttemptAnswerResponse {

    private Integer questionId;

    private String questionContent;

    private ContentFormat questionFormatSnapshot;

    private Integer selectedOptionId;

    private List<Integer> selectedOptionIds;

    private String essayAnswer;

    private String audioAnswerUrl;

    private Boolean isCorrect;

    private Double score;

    private String feedback;

    private String gradingMethod;
}


