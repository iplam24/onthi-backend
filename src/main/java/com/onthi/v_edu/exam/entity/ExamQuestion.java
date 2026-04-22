package com.onthi.v_edu.exam.entity;

import com.onthi.v_edu.question.entity.Question;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "exam_questions")
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

    @Column(columnDefinition = "TEXT")
    private String contentSnapshot;

    public ExamQuestion(Exam exam, Question question) {
        this.exam = exam;
        this.question = question;
        this.id = new ExamQuestionId(
                exam.getId(),
                question.getId()
        );
    }
}