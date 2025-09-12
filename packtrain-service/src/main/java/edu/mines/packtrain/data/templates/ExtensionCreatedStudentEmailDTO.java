package edu.mines.packtrain.data.templates;

import lombok.Data;

@Data
public class ExtensionCreatedStudentEmailDTO {
    private String requester;
    private String courseName;
    private int extensionDays;
    private String assignmentName;
    private String instructor;
    private boolean extension;
}
