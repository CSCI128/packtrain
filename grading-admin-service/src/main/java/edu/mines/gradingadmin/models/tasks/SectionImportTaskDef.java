package edu.mines.gradingadmin.models.tasks;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@Table(name = "section_import_tasks")
@Entity(name = "section_import_task")
public class SectionImportTaskDef extends ScheduledTaskDef{
    @Column(name = "course_id")
    private UUID courseToImport;

    @Column(name = "canvas_id")
    private long canvasId;
}
