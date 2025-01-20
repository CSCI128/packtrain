package edu.mines.gradingadmin.models;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
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

    @ManyToOne(fetch = FetchType.EAGER) // Need to add Target Entity once Course is defined
    @JoinColumn(name = "course_id")
    private String courseId;

}
