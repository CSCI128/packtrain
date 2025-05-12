package edu.mines.gradingadmin.models;

import edu.mines.gradingadmin.models.enums.MigrationStatus;
import jakarta.persistence.*;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;
import java.util.UUID;
import java.util.List;


@Data
@Entity (name = "master_migration")
@Table (name = "master_migrations")
public class MasterMigration {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "date_started")
    private Instant dateStarted = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private MigrationStatus status = MigrationStatus.CREATED;

    @OneToMany(mappedBy = "masterMigration", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Migration> migrations;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", referencedColumnName = "cwid")
    @EqualsAndHashCode.Exclude
    private User createdByUser;

    @ManyToOne(optional = false)
    @JoinColumn(name = "course_id", referencedColumnName = "id")
    @EqualsAndHashCode.Exclude
    private Course course;
}
