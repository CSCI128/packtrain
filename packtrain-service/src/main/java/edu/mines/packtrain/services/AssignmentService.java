package edu.mines.packtrain.services;

import edu.mines.packtrain.data.AssignmentDTO;
import edu.mines.packtrain.events.NewTaskEvent;
import edu.mines.packtrain.managers.IdentityProvider;
import edu.mines.packtrain.managers.ImpersonationManager;
import edu.mines.packtrain.models.Assignment;
import edu.mines.packtrain.models.Course;
import edu.mines.packtrain.models.ExternalAssignment;
import edu.mines.packtrain.models.User;
import edu.mines.packtrain.models.enums.ExternalAssignmentType;
import edu.mines.packtrain.models.tasks.AssignmentsSyncTaskDef;
import edu.mines.packtrain.models.tasks.ScheduledTaskDef;
import edu.mines.packtrain.repositories.AssignmentRepo;
import edu.mines.packtrain.repositories.ExternalAssignmentRepo;
import edu.mines.packtrain.repositories.ScheduledTaskRepo;
import edu.mines.packtrain.services.external.CanvasService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AssignmentService {
    private final ScheduledTaskRepo<AssignmentsSyncTaskDef> taskRepo;
    private final AssignmentRepo assignmentRepo;
    private final CourseService courseService;
    private final ApplicationEventPublisher eventPublisher;
    private final ImpersonationManager impersonationManager;
    private final CanvasService canvasService;
    private final ExternalAssignmentRepo externalAssignmentRepo;

    public AssignmentService(ScheduledTaskRepo<AssignmentsSyncTaskDef> taskRepo, AssignmentRepo assignmentRepo, CourseService courseService, ApplicationEventPublisher eventPublisher, ImpersonationManager impersonationManager, CanvasService canvasService, ExternalAssignmentRepo externalAssignmentRepo) {
        this.taskRepo = taskRepo;
        this.assignmentRepo = assignmentRepo;
        this.courseService = courseService;
        this.eventPublisher = eventPublisher;
        this.impersonationManager = impersonationManager;
        this.canvasService = canvasService;
        this.externalAssignmentRepo = externalAssignmentRepo;
    }

    public Assignment getAssignmentById(String id) {
        Optional<Assignment> assignment = assignmentRepo.getAssignmentById(UUID.fromString(id));
        if (assignment.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("Assignment '%s' does not exist!", id));
        }

        return assignment.get();
    }

    public List<Assignment> getAllAssignmentsGivenCourse(Course course){
        return assignmentRepo.getAllAssignmentsByCourseId(course.getId());
    }

    public void syncAssignmentTask(AssignmentsSyncTaskDef task){
        if(!task.shouldUpdateAssignments() && !task.shouldDeleteAssignments() && !task.shouldAddNewAssignments()){
            log.warn("No assignment sync action should be taken. Skipping task.");
            return;
        }

        Course course = courseService.getCourse(task.getCourseToSync());

        IdentityProvider impersonatedUser = impersonationManager.impersonateUser(task.getCreatedByUser());

        Map<Long, String> assignmentGroups = canvasService.asUser(impersonatedUser).getAssignmentGroups(course.getCanvasId());
        List<edu.ksu.canvas.model.assignment.Assignment> assignments = canvasService.asUser(impersonatedUser).getCourseAssignments(course.getCanvasId());

        Set<Long> incomingAssignments = assignments.stream().map(edu.ksu.canvas.model.assignment.Assignment::getId).collect(Collectors.toSet());

        Set<Long> existingAssignments = assignmentRepo.getAssignmentIdsByCourse(course);

        Set<Long> assignmentsToCreate = incomingAssignments.stream().filter(id -> !existingAssignments.contains(id)).collect(Collectors.toSet());
        Set<Long> assignmentsToRemove = existingAssignments.stream().filter(id -> !incomingAssignments.contains(id)).collect(Collectors.toSet());
        Set<Long> assignmentsToUpdate = incomingAssignments.stream().filter(id -> !assignmentsToCreate.contains(id)).collect(Collectors.toSet());

        if (task.shouldAddNewAssignments()){
            Set<Assignment> newAssignments = createNewAssignments(
                    assignmentGroups,
                    assignments.stream().filter(a -> assignmentsToCreate.contains(a.getId())).toList(),
                    course
            );
            log.info("Saving {} new assignments for '{}'", newAssignments.size(), course.getCode());

            assignmentRepo.saveAll(newAssignments);
        }

        if (task.shouldDeleteAssignments()){
            log.info("Deleting {} assignments for '{}'", assignmentsToRemove.size(), course.getCode());
            if (!assignmentsToRemove.isEmpty()) {
                assignmentRepo.deleteByCourseAndCanvasId(course, assignmentsToRemove);
            }
        }

        if (task.shouldAddNewAssignments()){
            Set<Assignment> updatedMembers = Set.of();
            log.info("Updating {} assignments for '{}'", updatedMembers.size(), course.getCode());

            if (!updatedMembers.isEmpty()) {
                assignmentRepo.saveAll(updatedMembers);
            }
        }

    }

    private Set<Assignment> createNewAssignments(Map<Long, String> assignmentGroups, List<edu.ksu.canvas.model.assignment.Assignment> canvasAssignments, Course course){
        Set<Assignment> assignments = new HashSet<>();

        for (edu.ksu.canvas.model.assignment.Assignment assignment : canvasAssignments){
            Assignment a = new Assignment();
            a.setName(assignment.getName());
            a.setCanvasId(assignment.getId());
            a.setPoints(assignment.getPointsPossible());

            if (assignment.getAssignmentGroupId() != null && assignmentGroups.containsKey(assignment.getAssignmentGroupId())){
                a.setCategory(assignmentGroups.get(assignment.getAssignmentGroupId()));
            }

            if (assignment.getDueAt() != null){
                a.setDueDate(assignment.getDueAt().toInstant());
            }

            if (assignment.getUnlockAt() != null){
                a.setUnlockDate(assignment.getUnlockAt().toInstant());
            }

            a.setEnabled(true);

            if (assignment.getGroupCategoryId() != null){
                a.setGroupAssignment(true);
            }

            if (assignment.getSubmissionTypes().isEmpty()){
                a.setAttentionRequired(true);
            }
            else if (assignment.getSubmissionTypes().getFirst().equals("none") ||
                    assignment.getSubmissionTypes().getFirst().equals("external_tool")){
                a.setAttentionRequired(true);
            }

            a.setCourse(course);
            assignments.add(a);
        }

        return assignments;
    }

    public ScheduledTaskDef syncAssignmentsFromCanvas(User actingUser, Set<Long> dependencies, UUID courseId, boolean addNew, boolean deleteOld, boolean updateExisting){
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

        eventPublisher.publishEvent(new NewTaskEvent(this, taskDefinition));

        return task;
    }

    public Assignment updateAssignment(String courseId, AssignmentDTO assignmentDTO) {
        Course course = courseService.getCourse(UUID.fromString(courseId));

        Assignment assignment = getAssignmentById(assignmentDTO.getId());

        ExternalAssignment externalServiceConfig = assignment.getExternalAssignmentConfig();
        if(assignment.getExternalAssignmentConfig() == null) {
            externalServiceConfig = new ExternalAssignment();
        }

        if(assignmentDTO.getExternalService() != null) {
            externalServiceConfig.setType(ExternalAssignmentType.valueOf(assignmentDTO.getExternalService()));
        }

        double externalPoints;
        if(assignmentDTO.getExternalPoints() == null) {
            externalPoints = 0.0;
        }
        else {
            externalPoints = assignmentDTO.getExternalPoints();
        }
        externalServiceConfig.setExternalPoints(externalPoints);

        assignment.setExternalAssignmentConfig(externalServiceConfig);
        externalAssignmentRepo.save(externalServiceConfig);

        assignment.setName(assignmentDTO.getName());
        assignment.setPoints(assignmentDTO.getPoints());
        assignment.setCategory(assignmentDTO.getCategory());
        assignment.setEnabled(assignmentDTO.getEnabled());
        assignment.setDueDate(assignmentDTO.getDueDate());
        assignment.setUnlockDate(assignmentDTO.getUnlockDate());
        assignment.setCourse(course);

        return assignmentRepo.save(assignment);
    }

    public Assignment addAssignmentToCourse(String courseId, AssignmentDTO assignmentDTO) {
        Course course = courseService.getCourse(UUID.fromString(courseId));

        Assignment assignment = new Assignment();
        assignment.setName(assignmentDTO.getName());
        assignment.setPoints(assignmentDTO.getPoints());
        assignment.setCategory(assignmentDTO.getCategory());
        assignment.setEnabled(assignmentDTO.getEnabled());
        assignment.setDueDate(assignmentDTO.getDueDate());
        assignment.setUnlockDate(assignmentDTO.getUnlockDate());
        assignment.setEnabled(true);
        assignment.setCourse(course);

        return assignmentRepo.save(assignment);
    }

    public List<Assignment> getAllUnlockedAssignments(String courseId) {
        List<Assignment> assignments = assignmentRepo.getAssignmentsByCourseId(UUID.fromString(courseId));
        Instant now = Instant.now();

        return assignments.stream()
                .filter(a -> a.getUnlockDate() == null || a.getUnlockDate().isBefore(now))
                .toList();
    }


    public Assignment enableAssignment(String assignmentId){
        Assignment assignment = getAssignmentById(assignmentId);

        assignment.setEnabled(true);

        return assignmentRepo.save(assignment);

    }

    public Assignment disableAssignment(String assignmentId){
        Assignment assignment = getAssignmentById(assignmentId);

        assignment.setEnabled(false);

        return assignmentRepo.save(assignment);
    }

    public Assignment getAssignmentForMigration(String migrationId) {
        Optional<Assignment> assignment = assignmentRepo.getAssignmentByMigrationId(UUID.fromString(migrationId));

        if (assignment.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("No assignment found for migration '%s'", migrationId));
        }

        return assignment.get();
    }
}
