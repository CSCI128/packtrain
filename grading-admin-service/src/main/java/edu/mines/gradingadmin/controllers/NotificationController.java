package edu.mines.gradingadmin.controllers;

import edu.mines.gradingadmin.managers.ImpersonationManager;
import edu.mines.gradingadmin.models.User;
import edu.mines.gradingadmin.services.TaskExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;

import static edu.mines.gradingadmin.factories.DTOFactory.toDto;

@Slf4j
@Controller
public class NotificationController {

    private final ImpersonationManager impersonationManager;
    private final TaskExecutorService taskExecutorService;

    public NotificationController(ImpersonationManager impersonationManager, TaskExecutorService taskExecutorService) {
        this.impersonationManager = impersonationManager;
        this.taskExecutorService = taskExecutorService;
    }

    @SubscribeMapping("/tasks/{task_id}")
    public String getTaskNotifications(Principal principal, @DestinationVariable("task_id") long taskId){
        User user = impersonationManager.impersonateUser(principal).getUser();

        log.debug("{} to {}", user.getCwid(), taskId);

        return String.valueOf(taskId);
    }


}
