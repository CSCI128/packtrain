package edu.mines.packtrain.data.templates;

import lombok.Data;

@Data
public class ExtensionCreatedInstructorEmailDTO {
    private String instructor;
    private String student;
    private String assignmentName;
    private String courseName;
    private int extensionDays;
    private String explanation;
    private String packtrainURL;
}
