package edu.mines.packtrain.services;

import edu.mines.packtrain.data.AssignmentDTO;
import edu.mines.packtrain.models.Assignment;
import edu.mines.packtrain.models.Course;
import edu.mines.packtrain.models.ExternalAssignment;
import edu.mines.packtrain.models.Migration;
import edu.mines.packtrain.models.enums.ExternalAssignmentType;
import edu.mines.packtrain.repositories.AssignmentRepo;
import edu.mines.packtrain.repositories.ExternalAssignmentRepo;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
public class AssignmentService {
    private final AssignmentRepo assignmentRepo;
    private final ExternalAssignmentRepo externalAssignmentRepo;

    public AssignmentService(AssignmentRepo assignmentRepo,
                             ExternalAssignmentRepo externalAssignmentRepo) {
        this.assignmentRepo = assignmentRepo;
        this.externalAssignmentRepo = externalAssignmentRepo;
    }

    public Assignment getAssignmentById(UUID id) {
        Optional<Assignment> assignment = assignmentRepo.getAssignmentById(id);
        if (assignment.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    String.format("Assignment '%s' does not exist!", id));
        }

        return assignment.get();
    }

    public List<Assignment> getAllAssignmentsGivenCourse(Course course) {
        return getAllAssignmentsGivenCourse(course, false);
    }

    public List<Assignment> getAllAssignmentsGivenCourse(Course course, boolean onlyMigratable) {
        if (onlyMigratable) {
            return assignmentRepo.getAllMigratableAssignmentsByCourse(course);
        }

        return assignmentRepo.getAssignmentByCourse(course);
    }

    public void createNewAssignmentsFromCanvas(Map<Long, String> assignmentGroups,
                                               List<edu.ksu.canvas.model.assignment.Assignment>
                                                       canvasAssignments, Course course) {
        Set<Assignment> assignments = new HashSet<>();

        for (edu.ksu.canvas.model.assignment.Assignment assignment : canvasAssignments) {
            Assignment a = new Assignment();
            a.setName(assignment.getName());
            a.setCanvasId(assignment.getId());
            a.setPoints(assignment.getPointsPossible());

            if (assignment.getAssignmentGroupId() != null
                    && assignmentGroups.containsKey(assignment.getAssignmentGroupId())) {
                a.setCategory(assignmentGroups.get(assignment.getAssignmentGroupId()));
            }

            if (assignment.getDueAt() != null) {
                a.setDueDate(assignment.getDueAt().toInstant());
            }

            if (assignment.getUnlockAt() != null) {
                a.setUnlockDate(assignment.getUnlockAt().toInstant());
            }

            a.setEnabled(true);

            if (assignment.getGroupCategoryId() != null) {
                a.setGroupAssignment(true);
            }

            if (assignment.getSubmissionTypes().isEmpty()) {
                a.setAttentionRequired(true);
            } else if (assignment.getSubmissionTypes().getFirst().equals("none") ||
                    assignment.getSubmissionTypes().getFirst().equals("external_tool")) {
                a.setAttentionRequired(true);
            }

            a.setCourse(course);
            assignments.add(a);
        }

        log.info("Saving {} new assignments for '{}'", assignments.size(), course.getCode());

        assignmentRepo.saveAll(assignments);
    }

    public void updateAssignmentsFromCanvas(Map<Long, String> assignmentGroups,
                                            Set<Long> assignmentCanvasIds,
                                            List<edu.ksu.canvas.model.assignment.Assignment>
                                                    canvasAssignments, Course course) {
        Set<Assignment> assignments = assignmentRepo.getAllByCourseAndCanvasId(course,
                assignmentCanvasIds);

        Set<Assignment> assignmentsToUpdate = new HashSet<>();

        for (Assignment assignment : assignments) {
            Optional<edu.ksu.canvas.model.assignment.Assignment> canvasAssignment =
                    canvasAssignments.stream()
                            .filter(a -> a.getId().equals(assignment.getCanvasId()))
                            .findFirst();
            if (canvasAssignment.isEmpty()) {
                log.warn("Requested to update assignment '{}', but no corresponding Canvas" +
                        " assignment was provided!", assignment.getName());
                continue;
            }

            assignment.setPoints(canvasAssignment.get().getPointsPossible());
            assignment.setName(canvasAssignment.get().getName());

            if (canvasAssignment.get().getAssignmentGroupId() != null
                    && assignmentGroups.containsKey(
                            canvasAssignment.get().getAssignmentGroupId())) {
                assignment.setCategory(assignmentGroups.get(canvasAssignment.get()
                        .getAssignmentGroupId()));
            }

            if (canvasAssignment.get().getDueAt() != null) {
                assignment.setDueDate(canvasAssignment.get().getDueAt().toInstant());
            }

            if (canvasAssignment.get().getUnlockAt() != null) {
                assignment.setUnlockDate(canvasAssignment.get().getUnlockAt().toInstant());
            }

            assignmentsToUpdate.add(assignment);
        }

        log.info("Updating {} assignments from canvas for course '{}'", assignmentsToUpdate.size(),
                course.getCode());

        assignmentRepo.saveAll(assignmentsToUpdate);
    }


    public void deleteAssignments(Set<Long> assignments, Course course) {
        log.info("Deleting {} assignments for course '{}'", assignments.size(), course.getCode());

        assignmentRepo.deleteByCourseAndCanvasId(course, assignments);
    }

    public Assignment updateAssignment(Course course, AssignmentDTO assignmentDTO) {
        Assignment assignment = getAssignmentById(assignmentDTO.getId());
        boolean shouldClearFlag = false;

        ExternalAssignment externalServiceConfig = assignment.getExternalAssignmentConfig();
        if (assignment.getExternalAssignmentConfig() == null) {
            externalServiceConfig = new ExternalAssignment();
        }

        if (assignmentDTO.getExternalService() != null) {
            externalServiceConfig.setType(ExternalAssignmentType.valueOf(assignmentDTO
                    .getExternalService()));
        }

        double externalPoints;
        if (assignmentDTO.getExternalPoints() == null) {
            externalPoints = 0.0;
        } else {
            externalPoints = assignmentDTO.getExternalPoints();
            shouldClearFlag = true;
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
        assignment.setGroupAssignment(assignmentDTO.getGroupAssignment());
        assignment.setCourse(course);

        if (assignment.isAttentionRequired() && shouldClearFlag) {
            assignment.setAttentionRequired(false);
        }

        return assignmentRepo.save(assignment);
    }

    public Assignment addAssignmentToCourse(Course course, AssignmentDTO assignmentDTO) {
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

    public List<Assignment> getAllUnlockedAssignments(Course course) {
        List<Assignment> assignments = assignmentRepo.getAssignmentByCourse(course);
        Instant now = Instant.now();

        return assignments.stream()
                .filter(a -> (a.getUnlockDate() == null
                        || a.getUnlockDate().isBefore(now)) && a.isEnabled())
                .toList();
    }


    public Assignment enableAssignment(UUID assignmentId) {
        Assignment assignment = getAssignmentById(assignmentId);

        assignment.setEnabled(true);

        return assignmentRepo.save(assignment);
    }

    public Assignment disableAssignment(UUID assignmentId) {
        Assignment assignment = getAssignmentById(assignmentId);

        assignment.setEnabled(false);

        return assignmentRepo.save(assignment);
    }

    public Assignment getAssignmentForMigration(Migration migration) {
        Optional<Assignment> assignment = assignmentRepo.getAssignmentByMigration(migration);

        if (assignment.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("No assignment" +
                    " found for migration '%s'", migration.getId()));
        }

        return assignment.get();
    }

    public Set<Long> getAssignmentCanvasIdsByCourse(Course course) {
        return assignmentRepo.getAssignmentByCourse(course).stream()
                .map(Assignment::getCanvasId)
                .collect(Collectors.toSet());
    }
}
