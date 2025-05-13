package edu.mines.packtrain.models.tasks;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@Table(name = "sync_course_tasks")
@Entity(name = "sync_course_task")
public class CourseSyncTaskDef extends ScheduledTaskDef {
    @Column(name = "course_id")
    private UUID courseToSync;

    @Column(name = "canvas_id")
    private long canvasId;

    @Column(name = "overwrite_name")
    @Accessors(fluent = true)
    private boolean shouldOverwriteName = false;

    @Column(name = "overwrite_code")
    @Accessors(fluent = true)
    private boolean shouldOverwriteCode = false;
}
