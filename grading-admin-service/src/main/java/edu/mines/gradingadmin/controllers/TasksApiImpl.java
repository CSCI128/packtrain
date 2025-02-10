package edu.mines.gradingadmin.controllers;

import edu.mines.gradingadmin.api.TasksApi;
import edu.mines.gradingadmin.api.TasksApiDelegate;
import edu.mines.gradingadmin.data.Task;
import edu.mines.gradingadmin.managers.SecurityManager;
import edu.mines.gradingadmin.services.TaskExecutorService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import java.time.OffsetDateTime;
import java.util.List;

@Controller
public class TasksApiImpl implements TasksApiDelegate {

    private final SecurityManager securityManager;
    private final TaskExecutorService taskExecutorService;

    public TasksApiImpl(SecurityManager securityManager, TaskExecutorService taskExecutorService) {
        this.securityManager = securityManager;
        this.taskExecutorService = taskExecutorService;
    }

    @Override
    public ResponseEntity<List<Task>> getAllTasksForUser() {
        return ResponseEntity.ok(taskExecutorService.getScheduledTasks(securityManager.getUser()).stream()
                .map(t -> new Task()
                        .id(t.getId())
                        .submittedTime(OffsetDateTime.from(t.getSubmittedTime()))
                        .completedTime(t.getCompletedTime() == null ? null : OffsetDateTime.from(t.getCompletedTime()))
                        .status(t.getStatus().toString())
                        .message(t.getStatusText())
        ).toList());
    }

    @Override
    public ResponseEntity<Task> getTask(Long taskId) {
        var task = taskExecutorService.getScheduledTask(securityManager.getUser(), taskId);

        if (task.isEmpty()){
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(task.map(t -> new Task()
                .id(t.getId())
                .submittedTime(OffsetDateTime.from(t.getSubmittedTime()))
                .completedTime(t.getCompletedTime() == null ? null : OffsetDateTime.from(t.getCompletedTime()))
                .status(t.getStatus().toString())
                .message(t.getStatusText())).get());
    }
}
