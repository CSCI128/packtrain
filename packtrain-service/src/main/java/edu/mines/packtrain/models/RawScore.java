package edu.mines.packtrain.models;

import edu.mines.packtrain.models.enums.SubmissionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Data;

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