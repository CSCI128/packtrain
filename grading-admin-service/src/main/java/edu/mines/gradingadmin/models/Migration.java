package edu.mines.gradingadmin.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;
import java.util.List;

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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "master_migration", referencedColumnName = "id")
    @EqualsAndHashCode.Exclude
    private MasterMigration masterMigration;

    // for now this relationship is one to one, later on this will be one to many
    // for now this relationship is one to one, later on this will be one to many
    // TO-DO: implement one to many so edge case like One Migration is for an assessment and reflection
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assignment", referencedColumnName = "id")
    @EqualsAndHashCode.Exclude
    private Assignment assignment;
}
