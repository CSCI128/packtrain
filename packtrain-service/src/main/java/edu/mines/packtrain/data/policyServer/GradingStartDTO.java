package edu.mines.packtrain.data.policyServer;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

@Data
public class GradingStartDTO {
    @AllArgsConstructor
    @Data
    public static class GlobalAssignmentMetadata {
        private UUID assignmentId;
        private double canvasMaxScore;
        private double canvasMinScore;
        private double externalMaxScore;
        private Instant initialDueDate;
    }

    private UUID migrationId;
    private URI policyURI;
    private String scoreCreatedRoutingKey;
    private String rawGradeRoutingKey;

    private GlobalAssignmentMetadata globalMetadata;

}
