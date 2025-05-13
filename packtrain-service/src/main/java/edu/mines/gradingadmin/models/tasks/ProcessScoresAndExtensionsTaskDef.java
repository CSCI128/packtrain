package edu.mines.gradingadmin.models.tasks;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.net.URI;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@Table(name = "process_scores_tasks")
@Entity(name = "process_score_task")
public class ProcessScoresAndExtensionsTaskDef extends ScheduledTaskDef{
    @Column(name = "migration_id", nullable = false)
    private UUID migrationId;
    @Column(name = "assignment_id", nullable = false)
    private UUID assignmentId;
    @Column(name = "policy_uri", nullable = false)
    private URI policy;
}
