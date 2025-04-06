package edu.mines.gradingadmin.models;

import edu.mines.gradingadmin.models.enums.SubmissionStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Entity(name = "raw_score")
@Table(name = "raw_scores")
// todo - this should be a composite key of cwid + migrationId
public class RawScore {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "migration_id", nullable = false)
    private UUID migrationId;

    @Column(name = "cwid", nullable = false)
    private String cwid;

    @Column(name = "score")
    private Double score;

    @Column(name = "submission_time")
    private Instant submissionTime;

    @Column(name = "hours_late")
    private Double hoursLate;

    @Enumerated(EnumType.STRING)
    @Column(name = "submission_status", nullable = false)
    private SubmissionStatus submissionStatus;
}