package edu.mines.packtrain.models.tasks;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@Table(name = "zero_out_submissions_task_defs")
@Entity(name = "zero_out_submissions_task_def")
public class ZeroOutSubmissionsTaskDef extends ScheduledTaskDef{
    @Column(name = "migration_id", nullable = false)
    private UUID migrationId;
}
