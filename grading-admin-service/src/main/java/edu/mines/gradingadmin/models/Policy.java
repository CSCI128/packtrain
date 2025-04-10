package edu.mines.gradingadmin.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@Data
@Entity(name = "policy")
@Table(name = "policies")
public class Policy {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", referencedColumnName = "cwid")
    @EqualsAndHashCode.Exclude
    private User createdByUser;

    @ManyToOne(optional = false)
    @JoinColumn(name = "course_id", referencedColumnName = "id")
    @EqualsAndHashCode.Exclude
    private Course course;

    @Column(name = "description")
    private String description;

    @Column(name = "policy_name", nullable = false)
    private String policyName;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "uri", unique = true, nullable = false)
    private String policyURI;

    @Column(name = "number_of_migrations", nullable = false)
    private int numberOfMigrations = 0;
}
