package edu.mines.packtrain.events;

import edu.mines.packtrain.models.tasks.ScheduledTaskDef;
import edu.mines.packtrain.repositories.ScheduledTaskRepo;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

@Getter
public class NewTaskEvent extends ApplicationEvent {
    @RequiredArgsConstructor
    @Setter
    @Getter
    public static class TaskData<T extends ScheduledTaskDef>{
        private final ScheduledTaskRepo<T> repo;

        private Set<Long> dependsOn = Set.of();

        private final Long taskId;

        private Optional<Consumer<T>> onJobStart = Optional.empty();
        private final Consumer<T> job;
        private Optional<Consumer<T>> onJobComplete = Optional.empty();
    }

    private final TaskData<?> data;

    public NewTaskEvent(Object source, TaskData<?> data) {
        super(source);
        this.data = data;
    }
}
