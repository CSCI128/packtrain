package edu.mines.gradingadmin.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;
import java.util.List;

@Data
@Entity(name = "Migration")
@Table(name = "Migrations")
public class Migration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @ManyToOne()
    @JoinColumn(name = "master_migration", referencedColumnName = "id")
    @EqualsAndHashCode.Exclude
    protected MasterMigration master_migration;

    // for now this relationship is one to one, later on this will be one to many
    // for now this relationship is one to one, later on this will be one to many
    // TO-DO: implement one to many so edge case like One Migration is for an assessment and reflection
    @OneToOne()
    @JoinColumn(name = "assignment", referencedColumnName = "id")
    @EqualsAndHashCode.Exclude
    protected Assignment assignment;

    // TO-DO: once raw shore sheet is created pull from that here too
    // many to one with a migration
    // many to one with extensions
}
