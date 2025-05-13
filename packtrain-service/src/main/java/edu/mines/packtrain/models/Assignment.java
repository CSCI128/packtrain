package edu.mines.packtrain.models;

import jakarta.persistence.*;
import lombok.Data;
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

    @Column(name ="canvas_id", unique = true)
    private long canvasId;

    @Column(name = "points")
    private double points;

    @Column(name = "due_date")
    private Instant dueDate;

    @Column(name = "unlock_date")
    private Instant unlockDate;

    @Column(name = "category")
    private String category;

    @Column(name = "enabled")
    private boolean enabled = true;

    @Column(name = "is_group_assignment")
    private boolean groupAssignment = false;

    @Column(name = "requires_attention")
    private boolean attentionRequired = false;

    @OneToOne(optional = true)
    @JoinColumn(name = "external_config", referencedColumnName = "id")
    private ExternalAssignment externalAssignmentConfig;

    @ManyToOne(optional = false)
    @JoinColumn(name = "course_id", referencedColumnName = "id")
    @ToString.Exclude
    private Course course;
}
