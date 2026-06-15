package com.onthi.v_edu.question.dto;

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
public class QuestionGroupResponse {
    private Integer id;
    private String title;
    private String content;
    private ContentFormat contentFormat;
    private String audioUrl;
    private List<QuestionResponse> questions;
    private Integer topicId;
    private String topicName;
    private Integer createdById;
    private String createdByUsername;
    private java.time.LocalDateTime createdAt;

    public QuestionGroupResponse(Integer id, String title, String content, ContentFormat contentFormat, String audioUrl,
                                 Integer topicId, String topicName, Integer createdById, String createdByUsername,
                                 java.time.LocalDateTime createdAt, java.time.LocalDateTime updatedAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.contentFormat = contentFormat;
        this.audioUrl = audioUrl;
        this.topicId = topicId;
        this.topicName = topicName;
        this.createdById = createdById;
        this.createdByUsername = createdByUsername;
        this.createdAt = createdAt;
    }
}
