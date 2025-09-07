package edu.mines.packtrain.controllers;

import edu.mines.packtrain.api.OwnerApiDelegate;
import edu.mines.packtrain.data.AssignmentDTO;
import edu.mines.packtrain.data.CourseDTO;
import edu.mines.packtrain.data.CourseMemberDTO;
import edu.mines.packtrain.data.CourseSyncTaskDTO;
import edu.mines.packtrain.data.PolicyDTO;
import edu.mines.packtrain.data.PolicyDryRunResultsDTO;
import edu.mines.packtrain.data.PolicyRawScoreDTO;
import edu.mines.packtrain.data.TaskDTO;
import edu.mines.packtrain.factories.DTOFactory;
import edu.mines.packtrain.managers.SecurityManager;
import edu.mines.packtrain.models.Assignment;
import edu.mines.packtrain.models.Course;
import edu.mines.packtrain.models.CourseMember;
import edu.mines.packtrain.models.Policy;
import edu.mines.packtrain.models.Section;
import edu.mines.packtrain.models.enums.CourseRole;
import edu.mines.packtrain.models.tasks.ScheduledTaskDef;
import edu.mines.packtrain.services.AssignmentService;
import edu.mines.packtrain.services.CourseMemberService;
import edu.mines.packtrain.services.CourseService;
import edu.mines.packtrain.services.PolicyService;
import edu.mines.packtrain.services.SectionService;
import edu.mines.packtrain.services.tasks.AssignmentTaskService;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class OwnerApiImpl implements OwnerApiDelegate {

    private final CourseService courseService;
    private final SectionService sectionService;
    private final CourseMemberService courseMemberService;
    private final PolicyService policyService;
    private final SecurityManager securityManager;
    private final AssignmentService assignmentService;
    private final AssignmentTaskService assignmentTaskService;

    public OwnerApiImpl(CourseService courseService, SectionService sectionService,
                        CourseMemberService courseMemberService,
                        AssignmentService assignmentService,
                        SecurityManager securityManager,
                        PolicyService policyService, AssignmentTaskService assignmentTaskService) {
        this.courseService = courseService;
        this.sectionService = sectionService;
        this.courseMemberService = courseMemberService;
        this.securityManager = securityManager;
        this.assignmentService = assignmentService;
        this.policyService = policyService;
        this.assignmentTaskService = assignmentTaskService;
    }

    @Override
    public ResponseEntity<Void> updateAssignment(UUID courseId, AssignmentDTO assignmentDto) {
        Course course = courseService.getCourse(courseId);
        Assignment assignment = assignmentService.updateAssignment(course, assignmentDto);

        return ResponseEntity.accepted().build();
    }

    @Override
    public ResponseEntity<AssignmentDTO> addAssignment(UUID courseId, AssignmentDTO assignmentDto) {
        Course course = courseService.getCourse(courseId);
        Assignment assignment = assignmentService.addAssignmentToCourse(course, assignmentDto);

        return ResponseEntity.accepted().body(DTOFactory.toDto(assignment));
    }

    @Override
    public ResponseEntity<List<TaskDTO>> syncCourse(UUID courseId, CourseSyncTaskDTO courseSyncTaskDTO) {
        return ResponseEntity.accepted().body(queueSyncCourseTasks(courseId, courseSyncTaskDTO));
    }

    @Override
    public ResponseEntity<List<TaskDTO>> importCourse(UUID courseId, CourseSyncTaskDTO courseSyncTaskDTO) {
        return ResponseEntity.accepted().body(queueSyncCourseTasks(courseId, courseSyncTaskDTO));
    }

    @Override
    public ResponseEntity<Void> updateCourse(UUID courseId, CourseDTO courseDTO) {
        Course course = courseService.updateCourse(courseId, courseDTO);

        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<CourseDTO> getCourse(UUID id, List<String> include) {
        if (include == null) {
            include = List.of();
        }

        Course course = courseService.getCourse(id);

        CourseDTO courseDto = DTOFactory.toDto(course);

        if (include.contains("members")) {
            courseDto.setMembers(courseMemberService.getAllMembersGivenCourse(course)
                    .stream().map(DTOFactory::toDto).toList());
        }

        if (include.contains("assignments")) {
            courseDto.setAssignments(assignmentService.getAllAssignmentsGivenCourse(course)
                    .stream().map(DTOFactory::toDto).toList());
        }

        if (include.contains("sections")) {
            courseDto.setSections(sectionService.getSectionsForCourse(course.getId())
                    .stream().map(Section::getName).toList());
        }

        return ResponseEntity.ok(courseDto);
    }

    @Override
    public ResponseEntity<List<CourseMemberDTO>> getMembers(UUID courseId,
                                                            List<String> enrollments,
                                                            String name, String cwid) {
        Course course = courseService.getCourse(courseId);

        if (name != null && cwid != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Must define one of " +
                    "'name' or 'cwid'");
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
        List<CourseMember> members = courseMemberService.searchCourseMembers(course, roles,
                name, cwid);
        members.forEach(m -> m.setSections(sectionService.getSectionByMember(m)));

        return ResponseEntity.ok(members
                .stream().map(DTOFactory::toDto).toList());
    }

    @Override
    public ResponseEntity<PolicyDTO> newPolicy(UUID courseId, String name, String filePath,
                                               MultipartFile fileData, String description) {
        Policy policy = policyService.createNewPolicy(securityManager.getUser(), courseId,
                name, description, filePath, fileData);

        return ResponseEntity.status(HttpStatus.CREATED).body(DTOFactory.toDto(policy));
    }

    @Override
    public ResponseEntity<Void> deletePolicy(UUID courseId, UUID policyId) {
        if (!policyService.deletePolicy(courseId, policyId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to delete policy!");
        }

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();

    }

    @Override
    public ResponseEntity<List<PolicyDTO>> ownerGetAllPolicies(UUID courseId) {
        List<Policy> policies = policyService.getAllPolicies(courseId);
        return ResponseEntity.ok(policies.stream().map(DTOFactory::toDto).toList());
    }

    @Override
    public ResponseEntity<Void> enableAssignment(UUID courseId, UUID assignmentId) {
        Assignment assignment = assignmentService.enableAssignment(assignmentId);

        return ResponseEntity.accepted().build();
    }

    @Override
    public ResponseEntity<Void> disableAssignment(UUID courseId, UUID assignmentId) {
        Assignment assignment = assignmentService.disableAssignment(assignmentId);

        return ResponseEntity.accepted().build();
    }

    @Override
    public ResponseEntity<PolicyDryRunResultsDTO> dryRunPolicy(UUID courseId,
                                                               MultipartFile fileData,
                                                               PolicyRawScoreDTO rawScore) {

        Optional<PolicyDryRunResultsDTO> res = policyService.dryRunPolicy(fileData, rawScore);

        if (res.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }


        return ResponseEntity.ok(res.get());
    }

    private List<TaskDTO> queueSyncCourseTasks(UUID courseId, CourseSyncTaskDTO courseSyncTaskDTO) {
        List<TaskDTO> tasks = new LinkedList<>();

        ScheduledTaskDef courseTask = courseService.syncCourseWithCanvas(
                securityManager.getUser(), courseId, courseSyncTaskDTO.getCanvasId(),
                courseSyncTaskDTO.getOverwriteName(), courseSyncTaskDTO.getOverwriteCode());

        tasks.add(DTOFactory.toDto(courseTask));

        ScheduledTaskDef sectionTask = sectionService.createSectionsFromCanvas(
                securityManager.getUser(), courseId, courseSyncTaskDTO.getCanvasId());


        tasks.add(DTOFactory.toDto(sectionTask));

        if (courseSyncTaskDTO.getImportUsers()) {
            ScheduledTaskDef importUsersTask = courseMemberService.syncMembersFromCanvas(
                    securityManager.getUser(), Set.of(courseTask.getId(), sectionTask.getId()),
                    courseId, true, true, true);

            tasks.add(DTOFactory.toDto(importUsersTask));
        }

        if (courseSyncTaskDTO.getImportAssignments()) {
            ScheduledTaskDef importAssignmentsTask = assignmentTaskService
                    .syncAssignmentsFromCanvas(securityManager.getUser(),
                            Set.of(courseTask.getId()), courseId,
                            true, true, true);

            tasks.add(DTOFactory.toDto(importAssignmentsTask));
        }

        return tasks;
    }
}
