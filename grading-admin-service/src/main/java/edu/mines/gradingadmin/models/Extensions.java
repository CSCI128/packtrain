package edu.mines.gradingadmin.models;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.UUID;

public class Extensions {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @OneToMany()
    @JoinColumn(name = "extension", referencedColumnName = "id")
    @EqualsAndHashCode.Exclude
    protected List<Extension> extensions;


    @ManyToOne()
    @JoinColumn(name = "assignment", referencedColumnName = "id")
    @EqualsAndHashCode.Exclude
    protected Assignment assignment;


}
