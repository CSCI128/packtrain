package edu.mines.packtrain.services.tasks;

import edu.ksu.canvas.model.assignment.Assignment;
import edu.mines.packtrain.events.NewTaskEvent;
import edu.mines.packtrain.managers.IdentityProvider;
import edu.mines.packtrain.managers.ImpersonationManager;
import edu.mines.packtrain.models.Course;
import edu.mines.packtrain.models.User;
import edu.mines.packtrain.models.tasks.AssignmentsSyncTaskDef;
import edu.mines.packtrain.models.tasks.ScheduledTaskDef;
import edu.mines.packtrain.repositories.ScheduledTaskRepo;
import edu.mines.packtrain.services.AssignmentService;
import edu.mines.packtrain.services.CourseService;
import edu.mines.packtrain.services.external.CanvasService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AssignmentTaskService {
    private final ScheduledTaskRepo<AssignmentsSyncTaskDef> taskRepo;
    private final ApplicationEventPublisher eventPublisher;
    private final CourseService courseService;
    private final ImpersonationManager impersonationManager;
    private final CanvasService canvasService;
    private final AssignmentService assignmentService;

    public AssignmentTaskService(ScheduledTaskRepo<AssignmentsSyncTaskDef> taskRepo, ApplicationEventPublisher eventPublisher, CourseService courseService, ImpersonationManager impersonationManager, CanvasService canvasService, AssignmentService assignmentService) {
        this.taskRepo = taskRepo;
        this.eventPublisher = eventPublisher;
        this.courseService = courseService;
        this.impersonationManager = impersonationManager;
        this.canvasService = canvasService;
        this.assignmentService = assignmentService;
    }

    public ScheduledTaskDef syncAssignmentsFromCanvas(User actingUser, Set<Long> dependencies, UUID courseId, boolean addNew, boolean deleteOld, boolean updateExisting, SimpMessagingTemplate messagingTemplate){
        AssignmentsSyncTaskDef task = new AssignmentsSyncTaskDef();
        task.setCourseToSync(courseId);
        task.setTaskName(String.format("Sync Course '%s': Course Assignments", courseId));
        task.setCreatedByUser(actingUser);
        task.shouldAddNewAssignments(addNew);
        task.shouldDeleteAssignments(deleteOld);
        task.shouldUpdateAssignments(updateExisting);
        task = taskRepo.save(task);

        NewTaskEvent.TaskData<AssignmentsSyncTaskDef> taskDefinition = new NewTaskEvent.TaskData<>(taskRepo, task.getId(), this::syncAssignmentTask);
        taskDefinition.setDependsOn(dependencies);
        taskDefinition.setOnJobComplete(Optional.of(_ ->
                messagingTemplate.convertAndSend("/courses/import", "assignments complete")));

        eventPublisher.publishEvent(new NewTaskEvent(this, taskDefinition));

        return task;
    }

    public void syncAssignmentTask(AssignmentsSyncTaskDef task){
        if(!task.shouldUpdateAssignments() && !task.shouldDeleteAssignments() && !task.shouldAddNewAssignments()){
            log.warn("No assignment sync action should be taken. Skipping task.");
            return;
        }

        Course course = courseService.getCourse(task.getCourseToSync());

        IdentityProvider impersonatedUser = impersonationManager.impersonateUser(task.getCreatedByUser());

        Map<Long, String> assignmentGroups = canvasService.asUser(impersonatedUser).getAssignmentGroups(course.getCanvasId());
        List<Assignment> assignments = canvasService.asUser(impersonatedUser).getCourseAssignments(course.getCanvasId());

        Set<Long> incomingAssignments = assignments.stream().map(Assignment::getId).collect(Collectors.toSet());

        Set<Long> existingAssignments = assignmentService.getAssignmentCanvasIdsByCourse(course);

        Set<Long> assignmentsToCreate = incomingAssignments.stream().filter(id -> !existingAssignments.contains(id)).collect(Collectors.toSet());
        Set<Long> assignmentsToRemove = existingAssignments.stream().filter(id -> !incomingAssignments.contains(id)).collect(Collectors.toSet());
        Set<Long> assignmentsToUpdate = incomingAssignments.stream().filter(id -> !assignmentsToCreate.contains(id)).collect(Collectors.toSet());

        if (task.shouldAddNewAssignments()){
            assignmentService.createNewAssignmentsFromCanvas(
                    assignmentGroups,
                    assignments.stream().filter(a -> assignmentsToCreate.contains(a.getId())).toList(),
                    course
            );
        }

        if (task.shouldDeleteAssignments()){
            if (!assignmentsToRemove.isEmpty()){
                assignmentService.deleteAssignments(assignmentsToRemove, course);
            }
        }

        if (task.shouldUpdateAssignments()){
            if (!assignmentsToUpdate.isEmpty()) {
                assignmentService.updateAssignmentsFromCanvas(
                        assignmentGroups,
                        assignmentsToUpdate,
                        assignments.stream().filter(a -> assignmentsToUpdate.contains(a.getId())).toList(),
                        course
                );
            }
        }
    }
}
