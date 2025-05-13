package edu.mines.gradingadmin.models.tasks;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@Table(name = "post_to_canvas_task_defs")
@Entity(name = "post_to_canvas_task_def")
public class PostToCanvasTaskDef extends ScheduledTaskDef {
    @Column(name = "migation_id")
    private UUID migrationId;

    @Column(name = "canvas_assignment_id")
    private long canvasAssignmentId;

    @Column(name = "canvas_course_id")
    private long canvasCourseId;
}
