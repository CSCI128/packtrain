package edu.mines.gradingadmin.controllers;

import edu.mines.gradingadmin.api.AdminApiDelegate;
import edu.mines.gradingadmin.data.*;
import edu.mines.gradingadmin.factories.DTOFactory;
import edu.mines.gradingadmin.managers.SecurityManager;
import edu.mines.gradingadmin.models.*;
import edu.mines.gradingadmin.models.enums.CourseRole;
import edu.mines.gradingadmin.models.tasks.ScheduledTaskDef;
import edu.mines.gradingadmin.services.*;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Transactional
@Controller
@Slf4j
public class AdminApiImpl implements AdminApiDelegate {

    private final CourseService courseService;
    private final SectionService sectionService;
    private final ExtensionService extensionService;
    private final CourseMemberService courseMemberService;
    private final AssignmentService assignmentService;
    private final SecurityManager securityManager;
    private final UserService userService;
    private final PolicyService policyService;

    public AdminApiImpl(CourseService courseService, SectionService sectionService, ExtensionService extensionService, CourseMemberService courseMemberService, AssignmentService assignmentService, SecurityManager securityManager, UserService userService, PolicyService policyService) {
        this.courseService = courseService;
        this.sectionService = sectionService;
        this.extensionService = extensionService;
        this.courseMemberService = courseMemberService;
        this.securityManager = securityManager;
        this.userService = userService;
        this.assignmentService = assignmentService;
        this.policyService = policyService;
    }

    @Override
    public ResponseEntity<Void> updateAssignment(String courseId, AssignmentDTO assignmentDto) {
        Optional<Assignment> assignment = assignmentService.updateAssignment(courseId, assignmentDto);

        if (assignment.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.accepted().build();
    }

    @Override
    public ResponseEntity<AssignmentDTO> addAssignment(String courseId, AssignmentDTO assignmentDto) {
        Optional<Assignment> assignment = assignmentService.addAssignmentToCourse(courseId, assignmentDto);

        if (assignment.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.accepted().body(DTOFactory.toDto(assignment.get()));
    }

    @Override
    public ResponseEntity<List<TaskDTO>> syncCourse(String courseId, CourseSyncTaskDTO courseSyncTaskDTO) {
        List<TaskDTO> tasks = new LinkedList<>();
        UUID courseUUID = UUID.fromString(courseId);

        Optional<ScheduledTaskDef> courseTask = courseService.syncCourseWithCanvas(
                securityManager.getUser(), courseUUID, courseSyncTaskDTO.getCanvasId(),
                courseSyncTaskDTO.getOverwriteName(), courseSyncTaskDTO.getOverwriteCode());

        if (courseTask.isEmpty()){
            return ResponseEntity.badRequest().build();
        }

        tasks.add(DTOFactory.toDto(courseTask.get()));

        Optional<ScheduledTaskDef> sectionTask = sectionService.createSectionsFromCanvas(
                securityManager.getUser(), courseUUID, courseSyncTaskDTO.getCanvasId());

        if (sectionTask.isEmpty()){
            return ResponseEntity.badRequest().build();
        }

        tasks.add(DTOFactory.toDto(sectionTask.get()));

        if (courseSyncTaskDTO.getImportUsers()){
            Optional<ScheduledTaskDef> importUsersTask = courseMemberService.syncMembersFromCanvas(securityManager.getUser(), Set.of(courseTask.get().getId(), sectionTask.get().getId()), courseUUID, true, true, true);

            if (importUsersTask.isEmpty()){
                return ResponseEntity.badRequest().build();
            }

            tasks.add(DTOFactory.toDto(importUsersTask.get()));
        }

        if (courseSyncTaskDTO.getImportAssignments()){
            // currently not doing the update because oh god reconciling that seems like a pain
            Optional<ScheduledTaskDef> importAssignmentsTask = assignmentService.syncAssignmentsFromCanvas(securityManager.getUser(), Set.of(courseTask.get().getId()), courseUUID, true, true, false);
            if (importAssignmentsTask.isEmpty()){
                return ResponseEntity.badRequest().build();
            }

            tasks.add(DTOFactory.toDto(importAssignmentsTask.get()));
        }

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(tasks);
    }

    @Override
    public ResponseEntity<List<TaskDTO>> importCourse(String courseId, CourseSyncTaskDTO courseSyncTaskDTO) {
        List<TaskDTO> tasks = new LinkedList<>();
        UUID courseUUID = UUID.fromString(courseId);

        Optional<ScheduledTaskDef> courseTask = courseService.syncCourseWithCanvas(
                securityManager.getUser(), courseUUID, courseSyncTaskDTO.getCanvasId(),
                courseSyncTaskDTO.getOverwriteName(), courseSyncTaskDTO.getOverwriteCode());

        if (courseTask.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        tasks.add(DTOFactory.toDto(courseTask.get()));

        Optional<ScheduledTaskDef> sectionTask = sectionService.createSectionsFromCanvas(
                securityManager.getUser(), courseUUID, courseSyncTaskDTO.getCanvasId());


        if (sectionTask.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        tasks.add(DTOFactory.toDto(sectionTask.get()));

        if (courseSyncTaskDTO.getImportUsers()) {
            Optional<ScheduledTaskDef> importUsersTask = courseMemberService.syncMembersFromCanvas(securityManager.getUser(), Set.of(courseTask.get().getId(), sectionTask.get().getId()), courseUUID, true, true, true);

            if (importUsersTask.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            tasks.add(DTOFactory.toDto(importUsersTask.get()));
        }

        if (courseSyncTaskDTO.getImportAssignments()){
            // currently not doing the update because oh god reconciling that seems like a pain
            Optional<ScheduledTaskDef> importAssignmentsTask = assignmentService.syncAssignmentsFromCanvas(securityManager.getUser(), Set.of(courseTask.get().getId()), courseUUID, true, true, false);
            if (importAssignmentsTask.isEmpty()){
                return ResponseEntity.badRequest().build();
            }

            tasks.add(DTOFactory.toDto(importAssignmentsTask.get()));
        }

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(tasks);
    }

    @Override
    public ResponseEntity<CourseDTO> newCourse(CourseDTO courseDTO) {
        Optional<Course> course = courseService.createNewCourse(courseDTO);
        if (course.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(DTOFactory.toDto(course.get()));
    }

    @Override
    public ResponseEntity<Void> updateCourse(String courseId, CourseDTO courseDTO) {
        Optional<Course> course = courseService.updateCourse(courseId, courseDTO);

        if (course.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<List<CourseDTO>> getCourses(Boolean enabled) {
        User user = securityManager.getUser();
        List<Course> courses = courseService.getCourses(enabled);
        return ResponseEntity.ok(courses.stream().map(DTOFactory::toDto).toList());
    }

    @Override
    public ResponseEntity<CourseDTO> getCourse(String id, List<String> include) {
        if (include == null) {
            include = List.of();
        }

        Optional<Course> course = courseService.getCourse(UUID.fromString(id));

        if (course.isEmpty()) {
            // need to do this with error controller
            return ResponseEntity.badRequest().build();
        }

        CourseDTO courseDto = DTOFactory.toDto(course.get());

        if (include.contains("members")) {
            courseDto.setMembers(course.get().getMembers().stream().map(DTOFactory::toDto).toList());
        }

        if (include.contains("assignments")) {
            courseDto.setAssignments(course.get().getAssignments().stream().map(DTOFactory::toDto).toList());
        }

        if (include.contains("sections")) {
            courseDto.setSections(course.get().getSections().stream().map(Section::getName).toList());
        }

        return ResponseEntity.ok(courseDto);
    }

    @Override
    public ResponseEntity<List<CourseMemberDTO>> getMembers(String courseId, List<String> enrollments, String name, String cwid) {
        Optional<Course> course = courseService.getCourse(UUID.fromString(courseId));

        if (course.isEmpty()) {
            // need to do this with error controller
            return ResponseEntity.badRequest().build();
        }

        if (name != null && cwid != null) {
            // need to do this with error controller
            return ResponseEntity.badRequest().build();
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

        return ResponseEntity.ok(courseMemberService.searchCourseMembers(course.get(), roles, name, cwid)
            .stream().map(DTOFactory::toDto).toList());
    }

    @Override
    public ResponseEntity<Void> addCourseMember(String courseId, CourseMemberDTO courseMemberDTO) {
        Optional<CourseMember> courseMember = courseMemberService.addMemberToCourse(courseId,
                courseMemberDTO);

        if (courseMember.isEmpty()) {
            // need to do this with error controller
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Override
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users.stream().map(DTOFactory::toDto).toList());
    }

    @Override
    public ResponseEntity<PolicyDTO> newPolicy(String courseId, String name, String filePath, MultipartFile fileData, String description) {
        Optional<Policy> policy = policyService.createNewPolicy(securityManager.getUser(), UUID.fromString(courseId), name, description, filePath, fileData);

        if (policy.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(DTOFactory.toDto(policy.get()));
    }

    @Override
    public ResponseEntity<Void> deletePolicy(String courseId, String policyId){
        if(!policyService.deletePolicy(UUID.fromString(courseId), UUID.fromString(policyId))){
            return ResponseEntity.badRequest().build();
        }


        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();

    }

    @Override
    public ResponseEntity<List<PolicyDTO>> adminGetAllPolicies(String courseId) {
        List<Policy> policies = policyService.getAllPolicies(UUID.fromString(courseId));
        return ResponseEntity.ok(policies.stream().map(DTOFactory::toDto).toList());
    }

    @Override
    public ResponseEntity<Void> enableAssignment(String courseId, String assignmentId) {
        Optional<Assignment> assignment = assignmentService.enableAssignment(assignmentId);
        if (assignment.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.accepted().build();
    }

    @Override
    public ResponseEntity<Void> disableAssignment(String courseId, String assignmentId) {
        Optional<Assignment> assignment = assignmentService.disableAssignment(assignmentId);
        if (assignment.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.accepted().build();
    }

    @Override
    public ResponseEntity<Void> createUser(UserDTO userDTO) {
        Optional<User> user = userService.createNewUser(
                userDTO.getCwid(),
                userDTO.getAdmin(),
                userDTO.getName(),
                userDTO.getEmail()
        );

        if (user.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Override
    public ResponseEntity<UserDTO> adminUpdateUser(UserDTO userDTO) {
        Optional<User> user;

        if(userDTO.getAdmin()) {
            user = userService.makeAdmin(userDTO.getCwid());
        } else {
            user = userService.demoteAdmin(securityManager.getUser(), userDTO.getCwid());
        }

        if (user.isEmpty()){
            return ResponseEntity.badRequest().build();
        }

        user = userService.updateUser(userDTO);

        if(userDTO.getEnabled()) {
            user = userService.enableUser(userDTO.getCwid());
        }
        else {
            user = userService.disableUser(securityManager.getUser(), userDTO.getCwid());
        }

        return ResponseEntity.accepted().body(DTOFactory.toDto(user.get()));
    }

    @Override
    public ResponseEntity<Void> enableUser(String cwid) {
        Optional<User> user = userService.enableUser(cwid);

        if (user.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.accepted().build();

    }

    @Override
    public ResponseEntity<Void> disableUser(String cwid) {
        Optional<User> user = userService.disableUser(securityManager.getUser(), cwid);

        if (user.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.accepted().build();
    }
}
