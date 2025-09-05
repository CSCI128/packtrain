package edu.mines.packtrain.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Data;

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
