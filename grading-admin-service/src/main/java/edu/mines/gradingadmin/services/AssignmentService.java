package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.events.NewTaskEvent;
import edu.mines.gradingadmin.managers.IdentityProvider;
import edu.mines.gradingadmin.managers.ImpersonationManager;
import edu.mines.gradingadmin.models.Assignment;
import edu.mines.gradingadmin.models.Course;
import edu.mines.gradingadmin.models.User;
import edu.mines.gradingadmin.models.tasks.AssignmentsSyncTaskDef;
import edu.mines.gradingadmin.models.tasks.ScheduledTaskDef;
import edu.mines.gradingadmin.repositories.AssignmentRepo;
import edu.mines.gradingadmin.repositories.ScheduledTaskRepo;
import edu.mines.gradingadmin.services.external.CanvasService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

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

    public AssignmentService(ScheduledTaskRepo<AssignmentsSyncTaskDef> taskRepo, AssignmentRepo assignmentRepo, CourseService courseService, ApplicationEventPublisher eventPublisher, ImpersonationManager impersonationManager, CanvasService canvasService) {
        this.taskRepo = taskRepo;
        this.assignmentRepo = assignmentRepo;
        this.courseService = courseService;
        this.eventPublisher = eventPublisher;
        this.impersonationManager = impersonationManager;
        this.canvasService = canvasService;
    }

    public Optional<Assignment> getAssignmentById(String id) {
        return assignmentRepo.getAssignmentById(UUID.fromString(id));
    }

    public void syncAssignmentTask(AssignmentsSyncTaskDef task){
        if(!task.shouldUpdateAssignments() && !task.shouldDeleteAssignments() && !task.shouldAddNewAssignments()){
            log.warn("No assignment sync action should be taken. Skipping task.");
            return;
        }

        Optional<Course> course = courseService.getCourse(task.getCourseToSync());
        if (course.isEmpty()){
            log.warn("Course '{}' does not exist!", task.getCourseToSync());
            return;
        }

        IdentityProvider impersonatedUser = impersonationManager.impersonateUser(task.getCreatedByUser());

        List<edu.ksu.canvas.model.assignment.Assignment> assignments = canvasService.asUser(impersonatedUser).getCourseAssignments(course.get().getCanvasId());

        Set<Long> incomingAssignments = assignments.stream().map(edu.ksu.canvas.model.assignment.Assignment::getId).collect(Collectors.toSet());

        Set<Long> existingAssignments = assignmentRepo.getAssignmentIdsByCourse(course.get());

        Set<Long> assignmentsToCreate = incomingAssignments.stream().filter(id -> !existingAssignments.contains(id)).collect(Collectors.toSet());
        Set<Long> assignmentsToRemove = existingAssignments.stream().filter(id -> !incomingAssignments.contains(id)).collect(Collectors.toSet());
        Set<Long> assignmentsToUpdate = incomingAssignments.stream().filter(id -> !assignmentsToCreate.contains(id)).collect(Collectors.toSet());

        if (task.shouldAddNewAssignments()){
            Set<Assignment> newAssignments = createNewAssignments(
                    assignments.stream().filter(a -> assignmentsToCreate.contains(a.getId())).toList(),
                    course.get()
            );
            log.info("Saving {} new assignments for '{}'", newAssignments.size(), course.get().getCode());

            assignmentRepo.saveAll(newAssignments);
        }

        if (task.shouldDeleteAssignments()){
            log.info("Deleting {} assignments for '{}'", assignmentsToRemove.size(), course.get().getCode());
            if (!assignmentsToRemove.isEmpty()) {
                assignmentRepo.deleteByCourseAndCanvasId(course.get(), assignmentsToRemove);
            }
        }

        if (task.shouldAddNewAssignments()){
            Set<Assignment> updatedMembers = Set.of();
            log.info("Updating {} assignments for '{}'", updatedMembers.size(), course.get().getCode());

            if (!updatedMembers.isEmpty()) {
                assignmentRepo.saveAll(updatedMembers);
            }
        }

    }

    private Set<Assignment> createNewAssignments(List<edu.ksu.canvas.model.assignment.Assignment> canvasAssignments, Course course){
        Set<Assignment> assignments = new HashSet<>();

        for (edu.ksu.canvas.model.assignment.Assignment assignment : canvasAssignments){
            Assignment a = new Assignment();
            a.setName(assignment.getName());
            a.setCanvasId(assignment.getId());
            a.setPoints(assignment.getPointsPossible());

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
                a.setRequiresAttention(true);
            }
            else if (assignment.getSubmissionTypes().getFirst().equals("none") ||
                    assignment.getSubmissionTypes().getFirst().equals("external_tool")){
                a.setRequiresAttention(true);
            }

            a.setCourse(course);
            assignments.add(a);
        }

        return assignments;
    }

    public Optional<ScheduledTaskDef> syncAssignmentsFromCanvas(User actingUser, Set<Long> dependencies, UUID courseId, boolean addNew, boolean deleteOld, boolean updateExisting){
        AssignmentsSyncTaskDef task = new AssignmentsSyncTaskDef();
        task.setCourseToSync(courseId);
        task.setCreatedByUser(actingUser);
        task.shouldAddNewAssignments(addNew);
        task.shouldDeleteAssignments(deleteOld);
        task.shouldUpdateAssignments(updateExisting);
        task = taskRepo.save(task);

        NewTaskEvent.TaskData<AssignmentsSyncTaskDef> taskDefinition = new NewTaskEvent.TaskData<>(taskRepo, task.getId(), this::syncAssignmentTask);
        taskDefinition.setDependsOn(dependencies);

        eventPublisher.publishEvent(new NewTaskEvent(this, taskDefinition));

        return Optional.of(task);
    }

    public Optional<Assignment> updateAssignment(String courseId, String assignmentId, String name, double points, String category, boolean enabled, Instant dueDate, Instant unlockDate) {
        Optional<Course> course = courseService.getCourse(UUID.fromString(courseId));
        if (course.isEmpty()) {
            return Optional.empty();
        }

        Optional<Assignment> assignment = getAssignmentById(assignmentId);
        if (assignment.isEmpty()) {
            return Optional.empty();
        }
        assignment.get().setName(name);
        assignment.get().setPoints(points);
        assignment.get().setCategory(category);
        assignment.get().setEnabled(enabled);
        assignment.get().setDueDate(dueDate);
        assignment.get().setUnlockDate(unlockDate);
        assignment.get().setCourse(course.get());

        return Optional.of(assignmentRepo.save(assignment.get()));
    }

    public Optional<Assignment> addAssignmentToCourse(String courseId, String name, double points, String category, Instant dueDate, Instant unlockDate) {
        Optional<Course> course = courseService.getCourse(UUID.fromString(courseId));
        if(course.isEmpty()) {
            return Optional.empty();
        }

        Assignment assignment = new Assignment();
        assignment.setName(name);
        assignment.setPoints(points);
        assignment.setCategory(category);
        assignment.setDueDate(dueDate);
        assignment.setUnlockDate(unlockDate);
        assignment.setEnabled(true);
        assignment.setCourse(course.get());

        return Optional.of(assignmentRepo.save(assignment));
    }

    public List<Assignment> getAllUnlockedAssignments(String courseId) {
        List<Assignment> assignments = assignmentRepo.getAssignmentsByCourseId(UUID.fromString(courseId));
        Instant now = Instant.now();

        return assignments.stream()
                .filter(a -> a.getUnlockDate() == null || a.getUnlockDate().isBefore(now))
                .toList();
    }


    public Optional<Assignment> enableAssignment(String assignmentId){
        Optional<Assignment> assignment = assignmentRepo.getAssignmentById(UUID.fromString(assignmentId));

        if (assignment.isEmpty()){
            return Optional.empty();
        }

        assignment.get().setEnabled(true);

        return Optional.of(assignmentRepo.save(assignment.get()));

    }

    public Optional<Assignment> disableAssignment(String assignmentId){
        Optional<Assignment> assignment = assignmentRepo.getAssignmentById(UUID.fromString(assignmentId));

        if (assignment.isEmpty()){
            return Optional.empty();
        }

        assignment.get().setEnabled(false);

        return Optional.of(assignmentRepo.save(assignment.get()));
    }
}
