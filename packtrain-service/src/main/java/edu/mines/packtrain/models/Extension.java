package edu.mines.packtrain.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity(name = "extension")
@Table(name = "extensions")
public class Extension {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "reason")
    private String reason;

    @Column(name = "comments")
    private String comments;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", referencedColumnName = "cwid")
    @EqualsAndHashCode.Exclude
    private User reviewer;

    @Column(name = "reviewer_response")
    private String reviewerResponse;

    @Column(name = "reviewer_response_timestamp")
    private Instant reviewerResponseTimestamp;
}
