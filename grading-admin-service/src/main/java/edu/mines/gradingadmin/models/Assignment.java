package edu.mines.gradingadmin.models;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity(name = "assignment")
@Table(name="assignments")
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "points")
    private double points;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "unlock_date")
    private LocalDateTime unlockDate;

    @Column(name = "category")
    private String category;

    @Column(name = "enabled")
    private boolean enabled;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "course_id", referencedColumnName = "id")
    private Course course;

}
