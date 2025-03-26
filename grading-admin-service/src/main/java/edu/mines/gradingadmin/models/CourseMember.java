package edu.mines.gradingadmin.models;


import edu.mines.gradingadmin.models.enums.CourseRole;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

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

    @Column(name="canvas_id", nullable = false)
    private String canvasId;

    @Enumerated(EnumType.STRING)
    @Column(name = "course_role", nullable = false)
    private CourseRole role;

    @Column(name = "late_passes_used", nullable = false)
    private int latePassesUsed = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "cwid", nullable = false)
    @EqualsAndHashCode.Exclude
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", referencedColumnName = "id")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Course course;

    @ManyToMany
    @JoinTable(name="member_section",
            joinColumns = @JoinColumn(name = "member_id", referencedColumnName = "id"),
            inverseJoinColumns  = @JoinColumn(name="section_id", referencedColumnName = "id")
    )
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<Section> sections;

}
