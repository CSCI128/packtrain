package edu.mines.packtrain.controllers;

import edu.mines.packtrain.api.OwnerApiDelegate;
import edu.mines.packtrain.data.*;
import edu.mines.packtrain.factories.DTOFactory;
import edu.mines.packtrain.managers.SecurityManager;
import edu.mines.packtrain.models.*;
import edu.mines.packtrain.models.enums.CourseRole;
import edu.mines.packtrain.models.tasks.ScheduledTaskDef;
import edu.mines.packtrain.services.*;
import edu.mines.packtrain.services.tasks.AssignmentTaskService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Controller
public class OwnerApiImpl implements OwnerApiDelegate {

    private final CourseService courseService;
    private final SectionService sectionService;
    private final CourseMemberService courseMemberService;
    private final PolicyService policyService;
    private final SecurityManager securityManager;
    private final AssignmentService assignmentService;
    private final AssignmentTaskService assignmentTaskService;

    public OwnerApiImpl(CourseService courseService, SectionService sectionService, CourseMemberService courseMemberService, AssignmentService assignmentService, SecurityManager securityManager, PolicyService policyService, AssignmentTaskService assignmentTaskService) {
        this.courseService = courseService;
        this.sectionService = sectionService;
        this.courseMemberService = courseMemberService;
        this.securityManager = securityManager;
        this.assignmentService = assignmentService;
        this.policyService = policyService;
        this.assignmentTaskService = assignmentTaskService;
    }

    @Override
    public ResponseEntity<Void> updateAssignment(String courseId, AssignmentDTO assignmentDto) {
        Course course = courseService.getCourse(UUID.fromString(courseId));
        Assignment assignment = assignmentService.updateAssignment(course, assignmentDto);

        return ResponseEntity.accepted().build();
    }

    @Override
    public ResponseEntity<AssignmentDTO> addAssignment(String courseId, AssignmentDTO assignmentDto) {
        Course course = courseService.getCourse(UUID.fromString(courseId));
        Assignment assignment = assignmentService.addAssignmentToCourse(course, assignmentDto);

        return ResponseEntity.accepted().body(DTOFactory.toDto(assignment));
    }

    @Override
    public ResponseEntity<List<TaskDTO>> syncCourse(String courseId, CourseSyncTaskDTO courseSyncTaskDTO) {
        List<TaskDTO> tasks = new LinkedList<>();
        UUID courseUUID = UUID.fromString(courseId);

        ScheduledTaskDef courseTask = courseService.syncCourseWithCanvas(
                securityManager.getUser(), courseUUID, courseSyncTaskDTO.getCanvasId(),
                courseSyncTaskDTO.getOverwriteName(), courseSyncTaskDTO.getOverwriteCode());


        tasks.add(DTOFactory.toDto(courseTask));

        ScheduledTaskDef sectionTask = sectionService.createSectionsFromCanvas(
                securityManager.getUser(), courseUUID, courseSyncTaskDTO.getCanvasId());

        tasks.add(DTOFactory.toDto(sectionTask));

        if (courseSyncTaskDTO.getImportUsers()) {
            ScheduledTaskDef importUsersTask = courseMemberService.syncMembersFromCanvas(securityManager.getUser(), Set.of(courseTask.getId(), sectionTask.getId()), courseUUID, true, true, true);

            tasks.add(DTOFactory.toDto(importUsersTask));
        }

        if (courseSyncTaskDTO.getImportAssignments()) {
            ScheduledTaskDef importAssignmentsTask = assignmentTaskService.syncAssignmentsFromCanvas(securityManager.getUser(), Set.of(courseTask.getId()), courseUUID, true, true, false);

            tasks.add(DTOFactory.toDto(importAssignmentsTask));
        }

        return ResponseEntity.accepted().body(tasks);
    }

    @Override
    public ResponseEntity<List<TaskDTO>> importCourse(String courseId, CourseSyncTaskDTO courseSyncTaskDTO) {
        List<TaskDTO> tasks = new LinkedList<>();
        UUID courseUUID = UUID.fromString(courseId);

        ScheduledTaskDef courseTask = courseService.syncCourseWithCanvas(
                securityManager.getUser(), courseUUID, courseSyncTaskDTO.getCanvasId(),
                courseSyncTaskDTO.getOverwriteName(), courseSyncTaskDTO.getOverwriteCode());

        tasks.add(DTOFactory.toDto(courseTask));

        ScheduledTaskDef sectionTask = sectionService.createSectionsFromCanvas(
                securityManager.getUser(), courseUUID, courseSyncTaskDTO.getCanvasId());


        tasks.add(DTOFactory.toDto(sectionTask));

        if (courseSyncTaskDTO.getImportUsers()) {
            ScheduledTaskDef importUsersTask = courseMemberService.syncMembersFromCanvas(securityManager.getUser(), Set.of(courseTask.getId(), sectionTask.getId()), courseUUID, true, true, true);

            tasks.add(DTOFactory.toDto(importUsersTask));
        }

        if (courseSyncTaskDTO.getImportAssignments()) {
            // currently not doing the update because oh god reconciling that seems like a pain
            ScheduledTaskDef importAssignmentsTask = assignmentTaskService.syncAssignmentsFromCanvas(securityManager.getUser(), Set.of(courseTask.getId()), courseUUID, true, true, false);

            tasks.add(DTOFactory.toDto(importAssignmentsTask));
        }

        return ResponseEntity.accepted().body(tasks);
    }

    @Override
    public ResponseEntity<Void> updateCourse(String courseId, CourseDTO courseDTO) {
        Course course = courseService.updateCourse(courseId, courseDTO);

        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<List<CourseDTO>> ownerGetCourses(Boolean enabled) {
        User user = securityManager.getUser();
        List<Course> courses = courseService.getCoursesByRole(securityManager.getUser(), CourseRole.OWNER, enabled);

        return ResponseEntity.ok(courses.stream().map(DTOFactory::toDto).toList());
    }

    @Override
    public ResponseEntity<CourseDTO> getCourse(String id, List<String> include) {
        if (include == null) {
            include = List.of();
        }

        Course course = courseService.getCourse(UUID.fromString(id));

        CourseDTO courseDto = DTOFactory.toDto(course);

        if (include.contains("members")) {
            courseDto.setMembers(courseMemberService.getAllMembersGivenCourse(course).stream().map(DTOFactory::toDto).toList());
        }

        if (include.contains("assignments")) {
            courseDto.setAssignments(assignmentService.getAllAssignmentsGivenCourse(course).stream().map(DTOFactory::toDto).toList());
        }

        if (include.contains("sections")) {
            courseDto.setSections(sectionService.getSectionsForCourse(course.getId()).stream().map(Section::getName).toList());
        }

        return ResponseEntity.ok(courseDto);
    }

    @Override
    public ResponseEntity<List<CourseMemberDTO>> getMembers(String courseId, List<String> enrollments, String name, String cwid) {
        Course course = courseService.getCourse(UUID.fromString(courseId));

        if (name != null && cwid != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Must define one of 'name' or 'cwid'");
        }

        List<CourseRole> roles = new ArrayList<>();
        if (enrollments == null) {
            roles = List.of(CourseRole.values());
        } else {
            if (enrollments.contains("tas")) {
                roles.add(CourseRole.TA);
            }

            if (enrollments.contains("instructors")) {
                roles.add(CourseRole.INSTRUCTOR);
                roles.add(CourseRole.OWNER);
            }

            if (enrollments.contains("students")) {
                roles.add(CourseRole.STUDENT);
            }
        }
        List<CourseMember> members = courseMemberService.searchCourseMembers(course, roles, name, cwid);
        members.forEach(m -> m.setSections(sectionService.getSectionByMember(m)));

        return ResponseEntity.ok(members
                .stream().map(DTOFactory::toDto).toList());
    }

    @Override
    public ResponseEntity<PolicyDTO> newPolicy(String courseId, String name, String filePath, MultipartFile fileData, String description) {
        Policy policy = policyService.createNewPolicy(securityManager.getUser(), UUID.fromString(courseId), name, description, filePath, fileData);

        return ResponseEntity.status(HttpStatus.CREATED).body(DTOFactory.toDto(policy));
    }

    @Override
    public ResponseEntity<Void> deletePolicy(String courseId, String policyId) {
        if (!policyService.deletePolicy(UUID.fromString(courseId), UUID.fromString(policyId))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to delete policy!");
        }

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();

    }

    @Override
    public ResponseEntity<List<PolicyDTO>> ownerGetAllPolicies(String courseId) {
        List<Policy> policies = policyService.getAllPolicies(UUID.fromString(courseId));
        return ResponseEntity.ok(policies.stream().map(DTOFactory::toDto).toList());
    }

    @Override
    public ResponseEntity<Void> enableAssignment(String courseId, String assignmentId) {
        Assignment assignment = assignmentService.enableAssignment(assignmentId);

        return ResponseEntity.accepted().build();
    }

    @Override
    public ResponseEntity<Void> disableAssignment(String courseId, String assignmentId) {
        Assignment assignment = assignmentService.disableAssignment(assignmentId);

        return ResponseEntity.accepted().build();
    }
}
