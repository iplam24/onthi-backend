package com.onthi.v_edu.exam.dto;

import com.onthi.v_edu.common.constant.DifficultyLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RandomExamRequest {

    @NotNull(message = "subjectId không được để trống")
    private Integer subjectId;

    @NotNull(message = "totalQuestions không được để trống")
    @Min(value = 1, message = "Tổng số câu hỏi phải >= 1")
    private Integer totalQuestions;

    @NotNull(message = "duration không được để trống")
    @Min(value = 1, message = "Thời gian làm bài phải >= 1 phút")
    private Integer duration;

    /**
     * Cấu hình phân bổ theo mức độ khó.
     * VD: [{difficulty: EASY, count: 10}, {difficulty: MEDIUM, count: 15}, {difficulty: HARD, count: 5}]
     * Nếu null/empty → phân bổ đều theo số câu có sẵn.
     */
    @Valid
    private List<DifficultyConfig> difficultyConfigs;

    /**
     * Cấu hình phân bổ theo chủ đề.
     * VD: [{topicId: 1, count: 10}, {topicId: 2, count: 15}]
     * Nếu null/empty → lấy random từ tất cả topics thuộc subject.
     */
    @Valid
    private List<TopicConfig> topicConfigs;

    /**
     * Cấu hình chi tiết theo chủ đề (dễ, trung bình, khó).
     */
    @Valid
    private List<TopicDetailedConfig> topicDetailedConfigs;

    /** Cho phép làm lại? Default: true */
    private Boolean allowRetake;

    /** Số lần làm tối đa. null = không giới hạn */
    private Integer maxAttempts;

    /** Tránh trùng câu hỏi với đề cũ của user. Default: true */
    private Boolean avoidDuplicates;

    /** Tiêu đề tùy chỉnh. null = auto generate */
    private String title;

    /** Tỷ lệ phần trăm trùng lặp tối đa với các đề AUTO cũ (0 - 100). null = không giới hạn */
    private Integer maxDuplicatePercentage;

    /** Có bao gồm các câu hỏi thuộc đoạn văn (Question Group) không. Default: false */
    private Boolean includeQuestionGroups;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DifficultyConfig {
        @NotNull(message = "difficulty không được để trống")
        private DifficultyLevel difficulty;

        @NotNull(message = "count không được để trống")
        @Min(value = 1, message = "Số lượng câu mỗi mức độ phải >= 1")
        private Integer count;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopicConfig {
        @NotNull(message = "topicId không được để trống")
        private Integer topicId;

        @NotNull(message = "count không được để trống")
        @Min(value = 1, message = "Số lượng câu mỗi chủ đề phải >= 1")
        private Integer count;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopicDetailedConfig {
        @NotNull(message = "topicId không được để trống")
        private Integer topicId;

        private Integer easyCount = 0;
        private Integer mediumCount = 0;
        private Integer hardCount = 0;
    }
}
