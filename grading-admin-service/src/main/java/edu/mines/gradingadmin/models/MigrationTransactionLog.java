package edu.mines.gradingadmin.models;

import edu.mines.gradingadmin.models.enums.SubmissionStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;
import java.util.UUID;

@Data
@Entity(name = "migration_transaction_log")
@Table(name = "migration_transaction_logs")
public class MigrationTransactionLog {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id")
    private long id;

    @ManyToOne()
    @JoinColumn(name = "created_by", referencedColumnName = "cwid", nullable = false)
    @EqualsAndHashCode.Exclude
    private User performedByUser;

    @Column(name = "student_cwid", nullable = false)
    private String cwid;

    @Column(name = "migration_id", nullable = false)
    private UUID migrationId;

    @Column(name = "revision", nullable = false)
    private int revision = 1;

    @Column(name = "extension_id")
    private UUID extensionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "submission_status", nullable = false)
    private SubmissionStatus submissionStatus;

    @Column(name = "extension_applied", nullable = false)
    private boolean extensionApplied = false;

    @Column(name = "score", nullable = false)
    private double score = 0;

    @Column(name = "submission_time")
    private Instant submissionTime;

    @Column(name = "message")
    private String message;
}
