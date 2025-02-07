package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.events.NewTaskEvent;
import edu.mines.gradingadmin.models.ScheduleStatus;
import edu.mines.gradingadmin.models.ScheduledTaskDef;
import edu.mines.gradingadmin.repositories.ScheduledTaskRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class TaskExecutorService implements ApplicationListener<NewTaskEvent> {
    private static final int MAX_ATTEMPTS = 10;
    private final ExecutorService executorService;

    public TaskExecutorService() {
        this.executorService = Executors.newFixedThreadPool(10);
    }

    public static <T extends ScheduledTaskDef> void runTask(NewTaskEvent.TaskData<T> taskData) {
        int curAttempts = 0;
        boolean errorFlag = false;
        String errorText="";

        ScheduledTaskRepo<T> taskRepo = taskData.getRepo();

        taskRepo.setStatus(taskData.getTaskId(), ScheduleStatus.QUEUED);

        do {
            List<ScheduleStatus> dependsOnStatus = taskData.getDependsOn().stream().map(id -> taskRepo.getStatus(id).orElse(ScheduleStatus.MISSING)).toList();

            if (dependsOnStatus.stream().anyMatch(t -> t == ScheduleStatus.FAILED)){
                errorFlag = true;
                errorText = "Dependent task failed.";
                break;
            }

            if (dependsOnStatus.stream().allMatch(t -> t == ScheduleStatus.COMPLETED)) {
                break;
            }
            curAttempts++;

            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e){

            }
        } while (curAttempts < MAX_ATTEMPTS);

        if (curAttempts == MAX_ATTEMPTS){
            errorFlag = true;
            errorText = "Too many attempts waiting for dependent tasks to complete.";
        }

        if (errorFlag){
            taskRepo.setStatus(taskData.getTaskId(), ScheduleStatus.FAILED, errorText);
            return;
        }


        taskRepo.setStatus(taskData.getTaskId(), ScheduleStatus.STARTED);

        T data = taskRepo.getById(taskData.getTaskId()).orElseThrow(RuntimeException::new);

        try{
            taskData.getOnJobStart().ifPresent(start -> start.accept(data));
        } catch (Exception e){
            errorFlag = true;
            errorText = String.format("Failed to run 'onJobStart' for task '{}'", taskData.getTaskId());
        }

        if (errorFlag){
            taskRepo.setStatus(taskData.getTaskId(), ScheduleStatus.FAILED, errorText);
            return;
        }

        try{
            taskData.getJob().accept(data);
        } catch (Exception e){
            errorFlag = true;
            errorText = String.format("Failed to run 'job' for task '{}'", taskData.getTaskId());
        }

        if (errorFlag){
            taskRepo.setStatus(taskData.getTaskId(), ScheduleStatus.FAILED, errorText);
            return;
        }

        try{
            taskData.getOnJobComplete().ifPresent(start -> start.accept(data));
        } catch (Exception e){
            errorFlag = true;
            errorText = String.format("Failed to run 'onJobComplete' for task '{}'", taskData.getTaskId());
        }

        if (errorFlag){
            taskRepo.setStatus(taskData.getTaskId(), ScheduleStatus.FAILED, errorText);
            return;
        }

        taskRepo.setStatus(taskData.getTaskId(), ScheduleStatus.COMPLETED);
        taskRepo.setCompletedTime(taskData.getTaskId(), Instant.now());
    }


    @Override
    public void onApplicationEvent(NewTaskEvent event) {
        executorService.submit(() -> runTask(event.getData()));
    }
}
