package edu.mines.gradingadmin.models;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Data
@Entity(name = "gradescope_config")
@Table(name = "gradescope_configs")
public class GradescopeConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "gradescopeId", unique = true, nullable = false)
    private String gradescopeId;
}
