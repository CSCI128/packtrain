package edu.mines.gradingadmin.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@Data
@Entity(name = "extension")
@Table(name="extensions")
public class Extension {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ExtensionStatus status;

    @Column(name = "assignment_id")
    private String assignment_id;

    @ManyToOne()
    @JoinColumn(name = "migration", referencedColumnName = "id")
    @EqualsAndHashCode.Exclude
    protected Migration migration;

    @ManyToOne()
    @JoinColumn(name = "extensions", referencedColumnName = "id")
    @EqualsAndHashCode.Exclude
    protected AssignmentExtensions extension_list;



}
