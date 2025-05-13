package edu.mines.packtrain.models.tasks;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@Table(name = "user_sync_tasks")
@Entity(name = "user_sync_task")
public class UserSyncTaskDef extends ScheduledTaskDef {
    @Column(name = "course_id")
    private UUID courseToImport;

    @Column(name = "assign_users_to_course")
    @Accessors(fluent = true)
    private boolean shouldAddNewUsers = false;

    @Column(name = "remove_old_users")
    @Accessors(fluent = true)
    private boolean shouldRemoveOldUsers = false;

    @Column(name = "update_exsiting_users")
    @Accessors(fluent = true)
    private boolean shouldUpdateExistingUsers = false;

}
