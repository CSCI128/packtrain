package edu.mines.packtrain.data.policyServer;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class GradingStartDTO {
    @AllArgsConstructor
    @Data
    public static class GlobalAssignmentMetadata {
        private UUID assignmentId;
        private double canvasMaxScore;
        private double canvasMinScore;
        private double externalMaxScore;
        private String initialDueDate;
    }

    private UUID migrationId;
    private URI policyURI;
    private String scoreCreatedRoutingKey;
    private String rawGradeRoutingKey;

    private GlobalAssignmentMetadata globalMetadata;

}
