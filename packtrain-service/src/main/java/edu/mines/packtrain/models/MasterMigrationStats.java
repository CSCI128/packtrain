package edu.mines.packtrain.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.util.UUID;

@Entity(name = "master_migration_stats")
@Table(name = "master_migration_stats_view")
@Data
public class MasterMigrationStats {
    @Id
    @Column(name = "master_migration_id")
    private UUID masterMigrationId;

    @Column(name = "total_submissions")
    private int totalSubmissions;

    @Column(name = "late_requests")
    private int lateRequests;

    @Column(name = "total_extensions")
    private int totalExtensions;

    @Column(name = "total_late_passes")
    private int totalLatePasses;

    @Column(name="unapproved_requests")
    private int unapprovedRequests;
}
