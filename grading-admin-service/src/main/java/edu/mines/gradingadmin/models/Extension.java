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
    /*
    @Column(name = "status")
    // create enum class for this in models
    private enum status;

    // need make a relationship by assignment id? fix this
    @Column(name = "assignment")
    private Assignment assignment;

    @ManyToOne()
    @JoinColumn(name = "migration", referencedColumnName = "id")
    @EqualsAndHashCode.Exclude
    protected Migration migration;

    @ManyToOne()
    @JoinColumn(name = "extensions", referencedColumnName = "id")
    @EqualsAndHashCode.Exclude
    protected AssignmentExtensions extension_list;
    */


}
