package edu.mines.gradingadmin.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.UUID;

@Data
@Entity(name = "assignment_extension")
@Table(name="assignment_extensions")
public class AssignmentExtensions {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @OneToMany()
    @JoinColumn(name = "extension", referencedColumnName = "id")
    @EqualsAndHashCode.Exclude
    private List<Extension> extensions;

    @OneToOne()
    @JoinColumn(name = "assignment", referencedColumnName = "id")
    @EqualsAndHashCode.Exclude
    private Assignment assignment;


}
