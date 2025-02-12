package edu.mines.gradingadmin.models.tasks;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@Table(name = "course_import_tasks")
@Entity(name = "course_import_task")
public class CourseImportTaskDef extends ScheduledTaskDef {
    @Column(name = "course_id")
    private UUID courseToImport;

    @Column(name = "canvas_id")
    private long canvasId;

    @Column(name = "overwrite_name")
    private boolean overwriteName = false;

    @Column(name = "overwrite_code")
    private boolean overwriteCode = false;
}
