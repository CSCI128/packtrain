package edu.mines.packtrain.data.policyServer;

import edu.mines.packtrain.models.enums.LateRequestStatus;
import edu.mines.packtrain.models.enums.SubmissionStatus;
import lombok.Data;

import java.time.Instant;

@Data
public class RawGradeDTO {
    private String cwid;
    private double rawScore;
    private Instant submissionDate;
    private SubmissionStatus submissionStatus;
    private String extensionId = null;
    private Instant extensionDate = null;
    private Double extensionDays = null;
    private String extensionType = null;
    private LateRequestStatus extensionStatus = null;
}
