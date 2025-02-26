package edu.mines.gradingadmin.models;

import jakarta.persistence.*;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;
import java.util.Date;
import java.util.List;


@Data
@Entity (name = "Master Migration")
@Table (name = "Master Migrations")
public class MasterMigration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "date_started")
    private Date date_started;

    @Column(name = "num_unapproved_extensions")
    private int num_unapproved_extensions;

    @Column(name = "num_late_submissions")
    private int num_late_submissions;

    @Column(name = "num_students")
    private int num_students;

    @Column(name = "num_extensions")
    private int num_extensions;

    @OneToMany()
    @JoinColumn(name = "migrations", referencedColumnName = "id")
    @EqualsAndHashCode.Exclude
    protected List<Migration> migrations;

    @ManyToOne()
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    @EqualsAndHashCode.Exclude
    protected User createdByUser;


}
