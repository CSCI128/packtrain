package edu.mines.gradingadmin.models;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Data
@Table(name = "course_import_tasks")
@Entity(name = "course_import_task")
public class CourseImportTaskDef extends ScheduledTaskDef {
    @Column(name = "course_id")
    private UUID courseToImport;

    @Column(name = "canvas_id")
    private String canvasId;

    @Column(name = "overwrite_name")
    private boolean overwriteName = false;

    @Column(name = "overwrite_code")
    private boolean overwriteCode = false;
}
