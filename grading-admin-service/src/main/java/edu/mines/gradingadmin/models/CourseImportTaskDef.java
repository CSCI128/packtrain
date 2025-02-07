package edu.mines.gradingadmin.models;

import jakarta.persistence.Inheritance;
import lombok.Data;

@Data
@Inheritance
public class CourseImportTask extends ScheduledTask{
}
