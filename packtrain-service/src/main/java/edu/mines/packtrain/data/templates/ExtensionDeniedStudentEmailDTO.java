package edu.mines.packtrain.data.templates;

import lombok.Data;

@Data
public class ExtensionDeniedStudentEmailDTO {
    private String reviewer;
    private String student;
    private String assignmentName;
    private String courseName;
    private int extensionDays;
    private String reviewerResponse;
}
