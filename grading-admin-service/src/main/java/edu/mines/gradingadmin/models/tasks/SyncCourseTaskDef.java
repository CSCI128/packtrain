package edu.mines.gradingadmin.models.tasks;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@Table(name = "sync_course_tasks")
@Entity(name = "sync_course_task")
public class SyncCourseTaskDef extends ScheduledTaskDef {
    @Column(name = "course_id")
    private UUID courseToImport;

    @Column(name = "canvas_id")
    private long canvasId;

    @Column(name = "overwrite_name")
    private boolean overwriteName = false;

    @Column(name = "overwrite_code")
    private boolean overwriteCode = false;
}
