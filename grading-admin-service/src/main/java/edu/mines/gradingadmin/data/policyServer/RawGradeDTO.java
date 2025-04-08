package edu.mines.gradingadmin.data.messages;

import edu.mines.gradingadmin.models.enums.LateRequestStatus;
import edu.mines.gradingadmin.models.enums.SubmissionStatus;
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
