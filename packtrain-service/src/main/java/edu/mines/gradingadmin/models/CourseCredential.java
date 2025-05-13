package edu.mines.gradingadmin.models;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Data
@Entity(name="course_credential")
@Table(name="course_credentials")
public class CourseCredential {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name="id")
    private UUID id;

    @ManyToOne
    @JoinColumn(name="course_id", referencedColumnName="id")
    private Course course;

    @ManyToOne
    @JoinColumn(name="credential_id", referencedColumnName = "id")
    private Credential credential;

    @Column(name="active")
    private boolean isActive;
}
