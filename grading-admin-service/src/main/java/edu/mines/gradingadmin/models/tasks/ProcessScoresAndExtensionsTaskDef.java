package edu.mines.gradingadmin.models.tasks;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@Table(name = "process_scores_tasks")
@Entity(name = "process_score_task")
public class ProcessScoresAndExtensionsTaskDef extends ScheduledTaskDef{
    private UUID migrationId;
    private UUID assignmentId;
}
