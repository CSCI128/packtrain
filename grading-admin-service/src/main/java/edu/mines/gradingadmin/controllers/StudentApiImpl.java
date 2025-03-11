package edu.mines.gradingadmin.controllers;

import edu.mines.gradingadmin.api.StudentApiDelegate;
import edu.mines.gradingadmin.data.*;
import edu.mines.gradingadmin.managers.SecurityManager;
import edu.mines.gradingadmin.models.*;
import edu.mines.gradingadmin.services.*;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import java.util.*;

@Transactional
@Controller
public class StudentApiImpl implements StudentApiDelegate {
    private final UserService userService;
    private final CourseService courseService;
    private final SectionService sectionService;
    private final ExtensionService extensionService;
    private final CourseMemberService courseMemberService;
    private final AssignmentService assignmentService;
    private final SecurityManager securityManager;

    public StudentApiImpl(UserService userService, CourseService courseService, SectionService sectionService, ExtensionService extensionService, CourseMemberService courseMemberService, AssignmentService assignmentService, SecurityManager securityManager) {
        this.userService = userService;
        this.courseService = courseService;
        this.sectionService = sectionService;
        this.extensionService = extensionService;
        this.courseMemberService = courseMemberService;
        this.assignmentService = assignmentService;
        this.securityManager = securityManager;
    }

    @Override
    public ResponseEntity<List<CourseDTO>> getCoursesStudent() {
        List<Course> courses = courseService.getCourses(true);

        List<CourseDTO> coursesResponse = courses.stream().map(course ->
                new CourseDTO()
                        .id(course.getId().toString())
                        .term(course.getTerm())
                        .enabled(course.isEnabled())
                        .name(course.getName())
                        .code(course.getCode())
        ).toList();

        return ResponseEntity.ok(coursesResponse);
    }

    @Override
    public ResponseEntity<List<CourseDTO>> getEnrollments() {
        User user = securityManager.getUser();

        Optional<List<Course>> enrollments = userService.getEnrollments(user.getCwid());

        if (enrollments.isEmpty()){
            return ResponseEntity.notFound().build();
        }

        List<CourseDTO> enrollmentsDto = enrollments.get().stream().map(enrollment ->
                new CourseDTO()
                        .name(enrollment.getName())
                        .term(enrollment.getTerm())
                        .code(enrollment.getCode())
        ).toList();

        return ResponseEntity.ok(enrollmentsDto);
    }

    @Override
    public ResponseEntity<StudentInformationDTO> getCourseInformationStudent(String courseId) {
        Optional<Course> course = courseService.getCourse(UUID.fromString(courseId));

        if(course.isEmpty()) {
            // need to do this with error controller
            return ResponseEntity.badRequest().build();
        }

        Set<Section> sections = courseMemberService.getSectionsForUserAndCourse(securityManager.getUser(), course.get());

        List<CourseRole> courseRoles = courseMemberService.getRolesForUserAndCourse(securityManager.getUser(), course.get().getId());

        CourseDTO courseDTO = new CourseDTO()
            .id(course.get().getId().toString())
            .code(course.get().getCode())
            .name(course.get().getName())
            .term(course.get().getTerm())
            .enabled(course.get().isEnabled())
            .canvasId(course.get().getCanvasId())
            .assignments(course.get().getAssignments().stream().filter(Assignment::isEnabled).map(assignment ->
                // "slim" assignmentdto, not all info is needed
                new AssignmentDTO()
                    .id(assignment.getId().toString())
                    .name(assignment.getName())
                    .dueDate(assignment.getDueDate())
                    .unlockDate(assignment.getUnlockDate())
                    .enabled(assignment.isEnabled())
                ).toList())
            .sections(sections.stream().map(Section::getName).toList());

        StudentInformationDTO studentInformationDTO = new StudentInformationDTO()
                .course(courseDTO)
                .professor("TODO implement this")
                .courseRole(StudentInformationDTO.CourseRoleEnum.fromValue(courseRoles.stream().findFirst().get().name().toLowerCase()));

        return ResponseEntity.ok(studentInformationDTO);
    }

    @Override
    public ResponseEntity<List<AssignmentDTO>> getCourseAssignmentsStudent(String courseId) {
        List<Assignment> assignments = assignmentService.getAllUnlockedAssignments(courseId);


        return ResponseEntity.ok(assignments.stream()
                .map(a -> new AssignmentDTO()
                        .points(a.getPoints())
                        .dueDate(a.getDueDate())
                        .unlockDate(a.getUnlockDate())
                        .category(a.getCategory())
                )
                .toList());
    }

    @Override
    public ResponseEntity<List<LateRequestDTO>> getAllExtensions(String courseId) {
        User user = securityManager.getUser();

        List<LateRequest> lateRequests = extensionService.getAllLateRequestsForStudent(courseId, user);

        return ResponseEntity.ok(lateRequests.stream().map(lateRequest ->
            new LateRequestDTO()
                .id(lateRequest.getId().toString())
                .numDaysRequested(lateRequest.getDaysRequested())
                .requestType(LateRequestDTO.RequestTypeEnum.fromValue(lateRequest.getRequestType().name().toLowerCase()))
                .status(LateRequestDTO.StatusEnum.fromValue(lateRequest.getStatus().name().toLowerCase()))
                .dateSubmitted(lateRequest.getSubmissionDate())
                .assignmentId(lateRequest.getAssignment().getId().toString())
                .assignmentName(lateRequest.getAssignment().getName())
                .extension(lateRequest.getExtension() != null ?
                    new ExtensionDTO()
                        .id(lateRequest.getExtension().getId().toString())
                        .reason(lateRequest.getExtension().getReason())
                        .comments(lateRequest.getExtension().getComments())
                        .responseToRequester(lateRequest.getExtension().getReviewerResponse())
                        .responseTimestamp(lateRequest.getExtension().getReviewerResponseTimestamp())
                    : null)
        ).toList());
    }

    @Override
    public ResponseEntity<LateRequestDTO> createExtensionRequest(String courseId, LateRequestDTO lateRequestDTO) {
        User user = securityManager.getUser();

        LateRequest lateRequest = extensionService.createLateRequest(
            courseId,
            user,
            lateRequestDTO.getRequestType(),
            lateRequestDTO.getNumDaysRequested(),
            lateRequestDTO.getDateSubmitted(),
            lateRequestDTO.getAssignmentId(),
            lateRequestDTO.getStatus(),
            lateRequestDTO.getExtension());

        return ResponseEntity.status(HttpStatus.CREATED).body(new LateRequestDTO()
            .id(lateRequest.getId().toString())
            .numDaysRequested(lateRequest.getDaysRequested())
            .requestType(LateRequestDTO.RequestTypeEnum.fromValue(lateRequest.getRequestType().name().toLowerCase()))
            .status(LateRequestDTO.StatusEnum.fromValue(lateRequest.getStatus().name().toLowerCase()))
            .dateSubmitted(lateRequest.getSubmissionDate())
            .extension(lateRequest.getExtension() != null ?
                new ExtensionDTO()
                    .id(lateRequest.getExtension().getId().toString())
                    .reason(lateRequest.getExtension().getReason())
                    .comments(lateRequest.getExtension().getComments())
                    .responseToRequester(lateRequest.getExtension().getReviewerResponse())
                    .responseTimestamp(lateRequest.getExtension().getReviewerResponseTimestamp())
                : null));
    }
}

