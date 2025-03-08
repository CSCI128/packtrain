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

    @Column(name = "reason")
    private String reason;

    @Column(name = "comments")
    private String comments;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "cwid", nullable = false)
    @EqualsAndHashCode.Exclude
    private User reviewer;

    @Column(name = "reviewer_response")
    private String reviewerResponse;

    @Column(name = "reviewer_response_timestamp")
    private Instant reviewerResponseTimestamp;
}
