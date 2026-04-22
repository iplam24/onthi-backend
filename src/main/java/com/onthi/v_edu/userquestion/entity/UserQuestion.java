package com.onthi.v_edu.userquestion.entity;
import com.onthi.v_edu.common.constant.DifficultyLevel;
import com.onthi.v_edu.common.constant.QuestionType;
import com.onthi.v_edu.learning.entity.Topic;
import com.onthi.v_edu.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@Entity
@Table(name = "user_questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    @Lob
    private String content;
    @Enumerated(EnumType.STRING)
    private QuestionType type;
    @Enumerated(EnumType.STRING)
    private DifficultyLevel difficulty;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private Topic topic;
    @Column(name = "is_public")
    private Boolean isPublic;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
