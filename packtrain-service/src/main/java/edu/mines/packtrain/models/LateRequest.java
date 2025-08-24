package edu.mines.packtrain.models;

import edu.mines.packtrain.models.enums.LateRequestStatus;
import edu.mines.packtrain.models.enums.LateRequestType;
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
    private LateRequestType lateRequestType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private LateRequestStatus status;

    @Column(name = "submission_date")
    private Instant submissionDate;

    @Column(name = "extension_date")
    private Instant extensionDate;

    @Column(name = "instructor")
    private String instructor;

    @OneToOne()
    @JoinColumn(name = "extension", referencedColumnName = "id")
    @EqualsAndHashCode.Exclude
    private Extension extension;

    @ManyToOne()
    @JoinColumn(name = "assignment", referencedColumnName = "id", nullable = false)
    @EqualsAndHashCode.Exclude
    private Assignment assignment;

    @ManyToOne()
    @JoinColumn(name = "migration", referencedColumnName = "id")
    @EqualsAndHashCode.Exclude
    private Migration migration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "cwid", nullable = false)
    @EqualsAndHashCode.Exclude
    private User requestingUser;
}
