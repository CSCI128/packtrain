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
    @Column(name = "id", unique = true, nullable = false)
    private UUID id;

    @Column(name = "migration_id", unique = true, nullable = false)
    private UUID migration_id;

    @Column(name = "assignment_id", unique = true, nullable = false)
    private UUID assignment_id;

    @Column(name = "cwid", unique = true, nullable = false)
    private String cwid;

    @Column(name = "score")
    private double score;

    @Column(name = "submission_time")
    private Instant submission_time;

    @Column(name = "hours_late")
    private Instant hours_late;

    @Column(name = "submission_status", nullable = false)
    private SubmissionStatus submissionStatus;
}