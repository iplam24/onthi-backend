package com.onthi.v_edu.classroom.entity;
import com.onthi.v_edu.user.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@Entity
@Table(name = "class_students")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClassStudent {
    @EmbeddedId
    private ClassStudentId id;
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("classId")
    @JoinColumn(name = "class_id")
    private SchoolClass schoolClass;
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("studentId")
    @JoinColumn(name = "student_id")
    private User student;
}
