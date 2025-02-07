package edu.mines.gradingadmin.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;
import java.util.UUID;

@Data
public abstract class ScheduledTask {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    protected UUID id;

    @ManyToOne()
    @JoinColumn(name = "user_id", referencedColumnName = "cwid")
    @EqualsAndHashCode.Exclude
    protected User createdByUser;

    @Column(name = "submitted_time")
    protected Instant submittedTime;

    @Column(name = "completed_time")
    protected Instant completedTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    protected ScheduleStatus status;
}
