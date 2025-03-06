package edu.mines.gradingadmin.models;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Entity(name = "raw_score")
@Table(name = "raw_scores")
public class RawScore {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", unique = true, nullable = false)
    private UUID id;

    @Column(name = "migration_id", nullable = false)
    private UUID migrationId;

    @Column(name = "assignment_id", nullable = false)
    private UUID assignmentId;

    @Column(name = "cwid", nullable = false)
    private String cwid;

    @Column(name = "score")
    private double score;

    @Column(name = "submission_time")
    private Instant submissionTime;

    @Column(name = "hours_late")
    private Instant hoursLate;

    @Column(name = "submission_status", nullable = false)
    private SubmissionStatus submissionStatus;
}