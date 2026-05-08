package com.onthi.v_edu.question.entity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
@Entity
@Table(name = "question_options")
@SQLDelete(sql = "UPDATE question_options SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuestionOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private Question question;
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String content;
    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(name = "deleted_at")
    private java.time.LocalDateTime deletedAt;
}
