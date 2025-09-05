package edu.mines.packtrain.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Data;

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
