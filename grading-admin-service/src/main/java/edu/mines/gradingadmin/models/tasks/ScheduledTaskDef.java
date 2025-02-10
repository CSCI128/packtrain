package edu.mines.gradingadmin.models.tasks;

import edu.mines.gradingadmin.models.User;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;

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

    @Column(name = "submitted_time")
    protected Instant submittedTime = Instant.now();

    @Column(name = "completed_time")
    protected Instant completedTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    protected ScheduleStatus status = ScheduleStatus.CREATED;


    @Column(name = "status_text")
    protected String statusText;
}
