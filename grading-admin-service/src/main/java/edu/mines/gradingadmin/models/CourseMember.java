package edu.mines.gradingadmin.models;


import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Set;
import java.util.UUID;

@Data
@Entity(name="course_member")
@Table(name="course_members")
public class CourseMember {
    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    @Column(name="id")
    private UUID id;

    @Column(name="canvas_id", unique = true, nullable = false)
    private String canvasId;

    @Enumerated(EnumType.STRING)
    @Column(name = "course_role", nullable = false)
    private CourseRole role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "cwid", nullable = false)
    @EqualsAndHashCode.Exclude
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", referencedColumnName = "id")
    @EqualsAndHashCode.Exclude
    private Course course;

    @ManyToMany(mappedBy = "members", cascade = {CascadeType.MERGE})
    @EqualsAndHashCode.Exclude
    private Set<Section> sections;

}
