package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.events.NewTaskEvent;
import edu.mines.gradingadmin.models.tasks.ScheduleStatus;
import edu.mines.gradingadmin.models.tasks.ScheduledTaskDef;
import edu.mines.gradingadmin.models.User;
import edu.mines.gradingadmin.repositories.ScheduledTaskRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class TaskExecutorService implements ApplicationListener<NewTaskEvent> {
    private static final int MAX_ATTEMPTS = 10;
    private final ExecutorService executorService;
    private final ScheduledTaskRepo<? extends ScheduledTaskDef> scheduledTaskRepo;

    public TaskExecutorService(ScheduledTaskRepo<? extends ScheduledTaskDef> scheduledTaskRepo) {
        this.scheduledTaskRepo = scheduledTaskRepo;
        this.executorService = Executors.newFixedThreadPool(10);
    }

    public static <T extends ScheduledTaskDef> void runTask(NewTaskEvent.TaskData<T> taskData) {
        ScheduledTaskRepo<T> taskRepo = taskData.getRepo();

        taskRepo.setStatus(taskData.getTaskId(), ScheduleStatus.QUEUED);

        if (!waitToStart(taskData, taskRepo)) return;

        if(!runJobs(taskData, taskRepo)) return;

        taskRepo.setStatus(taskData.getTaskId(), ScheduleStatus.COMPLETED);
        taskRepo.setCompletedTime(taskData.getTaskId(), Instant.now());
    }

    public static <T extends ScheduledTaskDef> boolean waitToStart(NewTaskEvent.TaskData<T> taskData, ScheduledTaskRepo<T> taskRepo) {
        int curAttempts = 0;

        do {
            List<ScheduleStatus> dependsOnStatus = taskData.getDependsOn().stream().map(id -> taskRepo.getStatus(id).orElse(ScheduleStatus.MISSING)).toList();

            if (dependsOnStatus.stream().anyMatch(t -> t == ScheduleStatus.FAILED)){
                taskRepo.setStatus(taskData.getTaskId(), ScheduleStatus.FAILED, "Dependent task failed.");
                return false;
            }

            if (dependsOnStatus.stream().allMatch(t -> t == ScheduleStatus.COMPLETED)) {
                break;
            }

            curAttempts++;

            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e){
                taskRepo.setStatus(taskData.getTaskId(), ScheduleStatus.FAILED,
                        String.format("An exception occurred when waiting for dependent task to complete:\n%s", e.getMessage()));
                return false;
            }
        } while (curAttempts < MAX_ATTEMPTS);

        if (curAttempts == MAX_ATTEMPTS){
            taskRepo.setStatus(taskData.getTaskId(), ScheduleStatus.FAILED,
                    "Too many attempts waiting for dependent tasks to complete.");
            return false;
        }

        return true;
    }

    public static <T extends ScheduledTaskDef> boolean runJobs(NewTaskEvent.TaskData<T> taskData, ScheduledTaskRepo<T> taskRepo) {
        taskRepo.setStatus(taskData.getTaskId(), ScheduleStatus.STARTED);

        T data = taskRepo.getById(taskData.getTaskId()).orElseThrow(RuntimeException::new);

        try{
            taskData.getOnJobStart().ifPresent(start -> start.accept(data));
        } catch (Exception e) {
            taskRepo.setStatus(taskData.getTaskId(), ScheduleStatus.FAILED,
                    String.format("Failed to run 'onJobStart' for task '%s':\n%s", taskData.getTaskId(), e.getMessage()));
            return false;
        }

        try{
            taskData.getJob().accept(data);
        } catch (Exception e){
            taskRepo.setStatus(taskData.getTaskId(), ScheduleStatus.FAILED,
                    String.format("Failed to run 'job' for task '%s':\n%s", taskData.getTaskId(), e.getMessage()));
            return false;
        }

        try{
            taskData.getOnJobComplete().ifPresent(start -> start.accept(data));
        } catch (Exception e){
            taskRepo.setStatus(taskData.getTaskId(), ScheduleStatus.FAILED,
                    String.format("Failed to run 'onJobComplete' for task '%s':\n%s", taskData.getTaskId(), e.getMessage()));
            return false;
        }

        return true;
    }


    @Override
    public void onApplicationEvent(NewTaskEvent event) {
        executorService.submit(() -> runTask(event.getData()));
    }

    public List<ScheduledTaskDef> getScheduledTasks(User currentUser) {
        return scheduledTaskRepo.getTasksForUser(currentUser).stream().map(t -> (ScheduledTaskDef) t).toList();
    }

    public Optional<ScheduledTaskDef> getScheduledTask(User currentUser, long taskId) {
        Optional<ScheduledTaskDef> task = scheduledTaskRepo.getById(taskId).map(t -> (ScheduledTaskDef) t);

        if (task.isEmpty() || !task.get().getCreatedByUser().equals(currentUser)){
            return Optional.empty();
        }

        return task;
    }


}
