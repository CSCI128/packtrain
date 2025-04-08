package edu.mines.gradingadmin.data.messages;

import edu.mines.gradingadmin.models.enums.LateRequestStatus;
import edu.mines.gradingadmin.models.enums.SubmissionStatus;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

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
