package com.onthi.v_edu.question.entity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@Entity
@Table(name = "explanations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Explanation {
    @Id
    @Column(name = "question_id")
    private Integer questionId;
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "question_id")
    private Question question;
    @Lob
    private String content;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
