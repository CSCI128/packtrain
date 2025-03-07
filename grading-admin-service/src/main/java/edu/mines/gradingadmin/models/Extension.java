package edu.mines.gradingadmin.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Entity(name = "extension")
@Table(name="extensions")
public class Extension {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ExtensionStatus status;

    @Column(name = "assignment_id")
    private String assignmentId;

    @Column(name = "extension_type")
    private ExtensionType extensionType;

    @Column(name = "days_extended")
    private int daysExtended;

    @Column(name = "submission_date")
    private Instant submissionDate;

    @Column(name = "new_due_date")
    private Instant newDueDate;

    @Column(name = "reason")
    private String reason; // TODO this should probably be an enum; or call this category

    @Column(name = "comments")
    private String comments;

    @ManyToOne()
    @JoinColumn(name = "migration", referencedColumnName = "id")
    @EqualsAndHashCode.Exclude
    private Migration migration;

    @ManyToOne()
    @JoinColumn(name = "extensions", referencedColumnName = "id")
    @EqualsAndHashCode.Exclude
    private AssignmentExtensions assignmentExtension;



}
