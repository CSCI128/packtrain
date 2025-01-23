package edu.mines.gradingadmin.models;

import jakarta.persistence.*;
import lombok.Data;
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

    @OneToMany(mappedBy = "assignment")
    private Set<Assignment> assignments;

}
