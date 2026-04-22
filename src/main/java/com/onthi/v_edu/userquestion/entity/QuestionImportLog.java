package com.onthi.v_edu.userquestion.entity;
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
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@Entity
@Table(name = "question_import_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuestionImportLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "user_question_id")
    private Integer userQuestionId;
    @Column(name = "imported_question_id")
    private Integer importedQuestionId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "imported_by")
    private User importedBy;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
