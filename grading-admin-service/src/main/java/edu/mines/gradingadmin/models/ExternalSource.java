package edu.mines.gradingadmin.models;

import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;

@Data
@Entity(name = "externalSource")
@Table(name = "external_sources")
public class ExternalSource {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ExternalSourceType type;

    @Column(name = "active")
    private boolean active = false;

    @Column(name = "endpoint", unique = true)
    private String endpoint;
}
