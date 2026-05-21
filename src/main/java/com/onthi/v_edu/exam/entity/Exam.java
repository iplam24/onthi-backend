package com.onthi.v_edu.exam.entity;
import com.onthi.v_edu.learning.entity.Subject;
import com.onthi.v_edu.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;

import java.time.LocalDateTime;

@Entity
@Table(name = "exams")
@SQLDelete(sql = "UPDATE exams SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Exam {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    private User createdBy;

    private Integer duration;

    private Boolean isActive;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private Double totalScore;

    private String type; // AUTO / MANUAL

    @Column(name = "is_public")
    private Boolean isPublic;

    private String uiLayoutHint;

    private Boolean shuffleQuestions;
    private Boolean shuffleAnswers;

    private Integer maxAttempts;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}