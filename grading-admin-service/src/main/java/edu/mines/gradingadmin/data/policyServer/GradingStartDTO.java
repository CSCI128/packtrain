package edu.mines.gradingadmin.data.messages;

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
        private double maxScore;
        private double minScore;
        private Instant initialDueDate;
    }

    private UUID migrationId;
    private URI policyURI;
    private String scoreCreatedRoutingKey;
    private String rawGradeRoutingKey;

    private GlobalAssignmentMetadata globalMetadata;

}
