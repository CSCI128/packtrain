package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.data.AssignmentDTO;
import edu.mines.gradingadmin.models.Assignment;
import edu.mines.gradingadmin.models.Course;
import edu.mines.gradingadmin.repositories.AssignmentRepo;
import edu.mines.gradingadmin.repositories.CourseMemberRepo;
import edu.mines.gradingadmin.repositories.ScheduledTaskRepo;
import jakarta.transaction.Transactional;
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

    @Transactional
    public Optional<Assignment> addAssignmentToCourse(String courseId, double points, String category, boolean enabled, Instant dueDate, Instant unlockDate) {
        Optional<Course> course = courseService.getCourse(UUID.fromString(courseId));
        if(course.isEmpty()) {
            return Optional.empty();
        }

        Assignment assignment = new Assignment();
        assignment.setPoints(points);
        assignment.setCategory(category);
        assignment.setEnabled(enabled);
        assignment.setDueDate(dueDate);
        assignment.setUnlockDate(unlockDate);
        assignment.setCourse(course.get());
        assignmentRepo.save(assignment);

        Set<Assignment> newAssignments;
        if(course.get().getAssignments() == null) {
            newAssignments = new HashSet<>();
        }
        else {
            newAssignments = new HashSet<>(course.get().getAssignments());
        }

        newAssignments.add(assignment);
        course.get().setAssignments(newAssignments);
        return Optional.of(assignment);
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
