package com.onthi.v_edu.exam.entity;

import com.onthi.v_edu.common.constant.ContentFormat;
import com.onthi.v_edu.question.entity.Question;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;

@Entity
@Table(name = "exam_questions")
@SQLDelete(sql = "UPDATE exam_questions SET deleted_at = CURRENT_TIMESTAMP WHERE exam_id = ? AND question_id = ?")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExamQuestion {

    @EmbeddedId
    private ExamQuestionId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("examId")
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("questionId")
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "order_index")
    private Integer orderIndex;

    private Double score;

    @Column(name = "section_name")
    private String sectionName;

    @Column(columnDefinition = "TEXT")
    private String contentSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_format_snapshot")
    private ContentFormat contentFormatSnapshot;

    @Column(name = "deleted_at")
    private java.time.LocalDateTime deletedAt;

    public ExamQuestion(Exam exam, Question question) {
        this.exam = exam;
        this.question = question;
        this.id = new ExamQuestionId(
                exam.getId(),
                question.getId()
        );
    }
}