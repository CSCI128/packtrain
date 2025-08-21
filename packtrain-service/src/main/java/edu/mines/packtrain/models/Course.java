package edu.mines.packtrain.models;

import java.util.Set;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

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

    @OneToOne(fetch = FetchType.EAGER, optional = true)
    @JoinColumn(name = "gradescope_config_id", referencedColumnName = "id")
    private GradescopeConfig gradescopeConfig;
}
