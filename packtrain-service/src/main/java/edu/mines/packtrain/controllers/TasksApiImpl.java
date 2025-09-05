package edu.mines.packtrain.controllers;

import edu.mines.packtrain.api.TasksApiDelegate;
import edu.mines.packtrain.data.TaskDTO;
import edu.mines.packtrain.factories.DTOFactory;
import edu.mines.packtrain.managers.SecurityManager;
import edu.mines.packtrain.models.tasks.ScheduledTaskDef;
import edu.mines.packtrain.services.TaskExecutorService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

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
        return ResponseEntity.ok(taskExecutorService.getScheduledTasks(securityManager.getUser())
                .stream().map(DTOFactory::toDto).toList());
    }

    @Override
    public ResponseEntity<TaskDTO> getTask(Long taskId) {
        ScheduledTaskDef task = taskExecutorService.getScheduledTask(securityManager.getUser(),
                taskId);

        return ResponseEntity.ok(DTOFactory.toDto(task));
    }
}
