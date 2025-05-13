package edu.mines.packtrain.models;

import edu.mines.packtrain.models.enums.ExternalAssignmentType;
import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Data
@Entity(name = "external_assignment")
@Table(name = "external_assignments")
public class ExternalAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "external_points")
    private double externalPoints;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private ExternalAssignmentType type;
}
