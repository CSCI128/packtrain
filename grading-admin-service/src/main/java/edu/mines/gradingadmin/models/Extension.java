package edu.mines.gradingadmin.models;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import org.checkerframework.checker.units.qual.C;

import java.util.List;
import java.util.UUID;

public class Extension {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "status")
    private String status;

    @Column(name = "assignment")
    private Assignment assignment;

    @ManyToOne()
    @JoinColumn(name = "migration", referencedColumnName = "id")
    @EqualsAndHashCode.Exclude
    protected Migration migration;

    @ManyToOne()
    @JoinColumn(name = "extensions", referencedColumnName = "id")
    @EqualsAndHashCode.Exclude
    protected Extensions extension_list;

    // TO-DO: once raw shore sheet is created pull from that here too
    // many to one with a migration
    // many to one with extensions



}
