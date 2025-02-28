package edu.mines.gradingadmin.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

@Data
@Entity(name = "assignment")
@Table(name="assignments")
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "name")
    private String name;

    @Column(name = "points")
    private double points;

    @Column(name = "due_date")
    private Instant dueDate;

    @Column(name = "unlock_date")
    private Instant unlockDate;

    @Column(name = "category")
    private String category;

    @Column(name = "enabled")
    private boolean enabled;

    @ManyToOne(optional = false)
    @JoinColumn(name = "course_id", referencedColumnName = "id")
    @ToString.Exclude
    private Course course;

    @OneToOne()
    @JoinColumn(name = "assignment_extensions", referencedColumnName = "id")
    @EqualsAndHashCode.Exclude
    protected AssignmentExtensions assignmentExtensions;

}
