package edu.mines.packtrain.models;

import edu.mines.packtrain.models.enums.ExternalAssignmentType;
import edu.mines.packtrain.models.enums.RawScoreStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity(name = "migration")
@Table(name = "migrations")
public class Migration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @ManyToOne()
    @JoinColumn(name = "policy", referencedColumnName = "id")
    @EqualsAndHashCode.Exclude
    private Policy policy;

    @ManyToOne()
//    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "master_migration", referencedColumnName = "id")
    @EqualsAndHashCode.Exclude
    private MasterMigration masterMigration;

    @Enumerated(EnumType.STRING)
    @Column(name = "raw_score_type")
    private ExternalAssignmentType rawScoreType;

    @Enumerated(EnumType.STRING)
    @Column(name = "raw_score_status", nullable = false)
    private RawScoreStatus rawScoreStatus = RawScoreStatus.EMPTY;

    @Column(name = "raw_score_message")
    private String rawScoreMessage;

    // for now this relationship is one to one, later on this will be one to many
    // for now this relationship is one to one, later on this will be one to many
    // TO-DO: implement one to many so edge case like One Migration is for an assessment
    // and reflection
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assignment", referencedColumnName = "id")
    @EqualsAndHashCode.Exclude
    private Assignment assignment;
}
