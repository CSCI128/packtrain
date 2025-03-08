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

    @Enumerated(EnumType.STRING)
    @Column(name = "extension_type")
    private ExtensionType extensionType;

    @Column(name = "days_extended")
    private int daysExtended;

    @Column(name = "submission_date")
    private Instant submissionDate;

    @Column(name = "new_due_date")
    private Instant newDueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason")
    private ExtensionReason reason;

    @Column(name = "comments")
    private String comments;

    @ManyToOne()
    @JoinColumn(name = "assignment", referencedColumnName = "id")
    @EqualsAndHashCode.Exclude
    private Assignment assignment;

    @ManyToOne()
    @JoinColumn(name = "migration", referencedColumnName = "id")
    @EqualsAndHashCode.Exclude
    private Migration migration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "cwid", nullable = false)
    @EqualsAndHashCode.Exclude
    private User user;
}
