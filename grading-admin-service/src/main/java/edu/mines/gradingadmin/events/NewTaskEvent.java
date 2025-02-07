package edu.mines.gradingadmin.events;

import edu.mines.gradingadmin.models.ScheduledTaskDef;
import edu.mines.gradingadmin.repositories.ScheduledTaskRepo;
import lombok.Builder;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public class NewTaskEvent extends ApplicationEvent {
    @Builder
    @Getter
    public static class TaskData<T extends ScheduledTaskDef>{
        private final ScheduledTaskRepo<T> repo;

        private Set<Long> dependsOn = Set.of();

        private Long taskId;

        private Optional<Consumer<T>> onJobStart = Optional.empty();
        private Consumer<T> job;
        private Optional<Consumer<T>> onJobComplete = Optional.empty();
    }

    @Getter
    final private TaskData<?> data;

    public NewTaskEvent(Object source, TaskData<?> data) {
        super(source);
        this.data = data;
    }
}
