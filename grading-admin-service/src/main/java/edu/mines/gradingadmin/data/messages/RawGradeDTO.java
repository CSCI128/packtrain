package edu.mines.gradingadmin.data.messages;

import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class RawGradeDTO {
    private String cwid;
    private UUID extensionId;
    private double rawScore;
    private Instant submissionDate;
    private Instant extensionDate;
    private double extensionHours;
    private String extensionType;
}
