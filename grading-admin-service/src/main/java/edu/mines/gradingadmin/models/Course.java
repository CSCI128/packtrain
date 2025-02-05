package edu.mines.gradingadmin.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.autoconfigure.web.WebProperties;

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

    @Column(name = "canvas_id")
    private String canvasId;

    @Column(name = "term")
    private String term;

    @Column(name = "enabled")
    private boolean enabled;

    @Column(name = "name")
    private String name;

    @Column(name = "code")
    private String code;

    @Column(name = "pages")
    private String pages;

    @OneToMany(mappedBy = "course")
    @EqualsAndHashCode.Exclude
    private Set<Assignment> assignments;

    @OneToMany(mappedBy = "course")
    @EqualsAndHashCode.Exclude
    private Set<Section> sections;

    @OneToMany(mappedBy = "course")
    @EqualsAndHashCode.Exclude
    private Set<CourseMember> members;
}
