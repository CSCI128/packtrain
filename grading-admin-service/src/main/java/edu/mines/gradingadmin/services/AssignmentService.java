package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.models.Assignment;
import edu.mines.gradingadmin.models.Course;
import edu.mines.gradingadmin.repositories.AssignmentRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
@Slf4j
public class AssignmentService {

    private final AssignmentRepo assignmentRepo;
    private final CourseService courseService;

    public AssignmentService(AssignmentRepo assignmentRepo, CourseService courseService) {
        this.assignmentRepo = assignmentRepo;
        this.courseService = courseService;
    }

    public Optional<Assignment> getAssignmentById(String id) {
        return assignmentRepo.getAssignmentById(UUID.fromString(id));
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
