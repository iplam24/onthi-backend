package com.onthi.v_edu.wallet.entity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Setter;
@Entity
@Table(name = "plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Plan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(unique = true, length = 50)
    private String name;
    @Column(precision = 15, scale = 2)
    private BigDecimal price;
    @Column(name = "duration_days")
    private Integer durationDays;

    @Column(name = "max_ai_questions_per_day")
    private Integer maxAiQuestionsPerDay;

    @Column(name = "max_ai_exams_per_month")
    private Integer maxAiExamsPerMonth;

    @Column(name = "has_ai_chatbot")
    private Boolean hasAiChatbot;

    @Column(name = "has_ai_grading")
    private Boolean hasAiGrading;

    @Column(name = "has_advanced_stats")
    private Boolean hasAdvancedStats;

    @Column(name = "has_custom_exams")
    private Boolean hasCustomExams;

    @Column(name = "has_ai_history")
    private Boolean hasAiHistory;

    @Column(name = "is_mentor_plan")
    private Boolean isMentorPlan;
}
