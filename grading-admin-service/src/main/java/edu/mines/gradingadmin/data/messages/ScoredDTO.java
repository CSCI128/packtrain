package edu.mines.gradingadmin.data.messages;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class ScoredDTO {
    public enum ExtensionStatus{
        IGNORED, APPLIED, REJECTED
    }

    public enum SubmissionStatus {
        MISSING, EXCUSED, LATE, EXTENDED, ON_TIME
    }

    private String cwid;
    private UUID extensionId;
    private double rawScore;
    private double finalScore;
    private Instant adjustedSubmissionTime;
    private double hoursLate;
    private SubmissionStatus submissionStatus;
    private ExtensionStatus extensionStatus;
    private String extensionMessage;
    private String submissionMessage;
}
