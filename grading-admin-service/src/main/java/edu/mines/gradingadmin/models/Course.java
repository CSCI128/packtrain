package edu.mines.gradingadmin.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Set;
import java.util.UUID;

@Data
@Entity(name = "course")
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "canvas_id", unique = true)
    private long canvasId;

    @Column(name = "term")
    private String term;

    @Column(name = "enabled")
    private boolean enabled;

    @Column(name = "name")
    private String name;

    @Column(name = "code")
    private String code;

    @OneToMany(mappedBy = "course")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<Assignment> assignments;

    @OneToMany(mappedBy = "course")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<Section> sections;

    @OneToMany(mappedBy = "course")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<CourseMember> members;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "extension_config_id", referencedColumnName = "id")
    private CourseLateRequestConfig lateRequestConfig;
}
