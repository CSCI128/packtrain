package edu.mines.gradingadmin.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.autoconfigure.web.WebProperties;

import java.util.Set;
import java.util.UUID;

@Data
@Entity(name = "section")
@Table(name = "sections")
public class Section {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "name")
    private String name;

    @Column(name = "canvas_id", unique = true)
    private long canvasId;

    @ManyToMany
    @JoinTable(name="section_member",
            joinColumns = @JoinColumn(name = "section_id", referencedColumnName = "id"),
            inverseJoinColumns  = @JoinColumn(name="student_id", referencedColumnName = "id")
    )
    @EqualsAndHashCode.Exclude
    private Set<CourseMember> members;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "course_id", referencedColumnName = "id")
    @EqualsAndHashCode.Exclude
    private Course course;

}
