package edu.mines.gradingadmin.models;

import jakarta.persistence.*;
import lombok.Data;
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

    @Column(name = "code")
    private String code;

    @OneToMany(mappedBy = "user")
    private Set<User> users;

//    @Enumerated(EnumType.STRING)
//    @Column(name = "course_role", nullable = false)
//    private CourseRole courseRole;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "course_id", referencedColumnName = "id")
    private Course course;

}
