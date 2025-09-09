package edu.mines.packtrain.data.policyServer;

import edu.mines.packtrain.models.enums.LateRequestStatus;
import edu.mines.packtrain.models.enums.SubmissionStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.Data;

@Data
public class ScoredDTO {
    private String cwid;
    private UUID extensionId;
    private double rawScore;
    private double finalScore;
    private Instant adjustedSubmissionTime;
    private double hoursLate;
    private SubmissionStatus submissionStatus;
    private LateRequestStatus extensionStatus;
    private String extensionMessage;
    private String submissionMessage;
}
