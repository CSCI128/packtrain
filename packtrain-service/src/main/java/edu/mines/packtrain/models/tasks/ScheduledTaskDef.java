package edu.mines.packtrain.models.tasks;

import edu.mines.packtrain.models.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.Instant;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class ScheduledTaskDef {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    protected long id;

    @ManyToOne()
    @JoinColumn(name = "user_id", referencedColumnName = "cwid")
    @EqualsAndHashCode.Exclude
    protected User createdByUser;

    @Column(name = "task_name", nullable = false)
    protected String taskName;

    @Column(name = "submitted_time")
    protected Instant submittedTime = Instant.now();

    @Column(name = "completed_time")
    protected Instant completedTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    protected ScheduleStatus status = ScheduleStatus.CREATED;

    @Column(name = "status_text", length = 10000)
    protected String statusText;
}
