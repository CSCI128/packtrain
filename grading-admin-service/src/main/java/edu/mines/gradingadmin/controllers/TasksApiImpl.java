package edu.mines.gradingadmin.controllers;

import edu.mines.gradingadmin.api.TasksApiDelegate;
import edu.mines.gradingadmin.data.TaskDTO;
import edu.mines.gradingadmin.managers.SecurityManager;
import edu.mines.gradingadmin.models.tasks.ScheduledTaskDef;
import edu.mines.gradingadmin.services.TaskExecutorService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Controller
public class TasksApiImpl implements TasksApiDelegate {

    private final SecurityManager securityManager;
    private final TaskExecutorService taskExecutorService;

    public TasksApiImpl(SecurityManager securityManager, TaskExecutorService taskExecutorService) {
        this.securityManager = securityManager;
        this.taskExecutorService = taskExecutorService;
    }

    @Override
    public ResponseEntity<List<TaskDTO>> getAllTasksForUser() {
        return ResponseEntity.ok(taskExecutorService.getScheduledTasks(securityManager.getUser()).stream()
                .map(t -> new TaskDTO()
                        .id(t.getId())
                        .submittedTime(t.getSubmittedTime())
                        .completedTime(t.getCompletedTime() == null ? null : t.getCompletedTime())
                        .status(t.getStatus().toString())
                        .message(t.getStatusText())
        ).toList());
    }

    @Override
    public ResponseEntity<TaskDTO> getTask(Long taskId) {
        Optional<ScheduledTaskDef> task = taskExecutorService.getScheduledTask(securityManager.getUser(), taskId);

        if (task.isEmpty()){
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(task.map(t -> new TaskDTO()
                .id(t.getId())
                .submittedTime(t.getSubmittedTime())
                .completedTime(t.getCompletedTime() == null ? null : t.getCompletedTime())
                .status(t.getStatus().toString())
                .message(t.getStatusText())).get());
    }
}
