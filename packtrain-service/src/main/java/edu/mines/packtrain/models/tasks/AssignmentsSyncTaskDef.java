package edu.mines.packtrain.models.tasks;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@EqualsAndHashCode(callSuper = true)
@Data
@Table(name = "sync_assignments_tasks")
@Entity(name = "sync_assignment_task")
public class AssignmentsSyncTaskDef extends ScheduledTaskDef {

    @Column(name = "course_id")
    private UUID courseToSync;

    @Column(name = "add_new_assignments")
    @Accessors(fluent = true)
    private boolean shouldAddNewAssignments;

    @Column(name = "delete_old_assignments")
    @Accessors(fluent = true)
    private boolean shouldDeleteAssignments;

    @Column(name = "update_existing_assignments")
    @Accessors(fluent = true)
    private boolean shouldUpdateAssignments;
}
