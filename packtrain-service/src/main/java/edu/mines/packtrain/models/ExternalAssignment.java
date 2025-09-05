package edu.mines.packtrain.models;

import edu.mines.packtrain.models.enums.ExternalAssignmentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Data;

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
