package com.onthi.v_edu.user.entity;
import com.onthi.v_edu.learning.entity.Level;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@Entity
@Table(name = "user_information")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserInformation {
    @jakarta.persistence.Id
    @Column(name = "user_id")
    private Integer userId;
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;
    @Column(name = "full_name", length = 150)
    private String fullName;
    @Column(name = "school_name", length = 255)
    private String schoolName;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "level_id")
    private Level level;
    private LocalDate dob;
    @Column(length = 255)
    private String avatar;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
