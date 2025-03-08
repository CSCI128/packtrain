package edu.mines.gradingadmin.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;
import java.util.UUID;

@Data
@Entity(name = "late_request")
@Table(name="late_requests")
public class LateRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "days_requested")
    private int daysRequested;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type")
    private RequestType requestType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private RequestStatus status;

    @Column(name = "submission_date")
    private Instant submissionDate;

    @OneToOne()
    @JoinColumn(name = "extension", referencedColumnName = "id")
    @EqualsAndHashCode.Exclude
    private Extension extension;

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
