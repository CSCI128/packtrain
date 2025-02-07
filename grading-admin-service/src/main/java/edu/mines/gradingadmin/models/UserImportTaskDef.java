package edu.mines.gradingadmin.models;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Set;
import java.util.UUID;

@Data
@Table(name = "user_import_tasks")
@Entity(name = "user_import_task")
public class UserImportTaskDef extends ScheduledTaskDef {
    @Column(name = "course_id")
    private UUID courseToImport;

    @ElementCollection
    @CollectionTable(joinColumns = @JoinColumn(name = "id"))
    private Set<UUID> sections;

    @Column(name = "assign_users_to_course")
    private boolean assignUsersToCourse = false;

}
