package edu.mines.gradingadmin.controllers;

import edu.mines.gradingadmin.api.StudentApiDelegate;
import edu.mines.gradingadmin.data.*;
import edu.mines.gradingadmin.managers.SecurityManager;
import edu.mines.gradingadmin.models.*;
import edu.mines.gradingadmin.services.*;
import jakarta.transaction.Transactional;
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
            .sections(sections.stream().map(Section::getName).toList());

        StudentInformationDTO studentInformationDTO = new StudentInformationDTO()
                .course(courseDTO)
                .professor("TODO implement this")
                .courseRole(StudentInformationDTO.CourseRoleEnum.fromValue(courseRoles.stream().findFirst().get().name()));

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

        List<LateRequest> lateRequests = extensionService.getAllLateRequestsForStudent(courseId, user.getCwid());

        return ResponseEntity.ok(lateRequests.stream().map(lateRequest ->
            new LateRequestDTO()
                .id(lateRequest.getId().toString())
                .numDaysRequested(lateRequest.getDaysRequested())
                .requestType(LateRequestDTO.RequestTypeEnum.fromValue(lateRequest.getRequestType().name()))
                .status(LateRequestDTO.StatusEnum.fromValue(lateRequest.getStatus().name()))
                .dateSubmitted(lateRequest.getSubmissionDate())
                .extension(lateRequest.getExtension() != null ?
                    List.of(new ExtensionDTO()
                        .id(lateRequest.getExtension().getId().toString())
                        .reason(ExtensionDTO.ReasonEnum.fromValue(lateRequest.getExtension().getReason().name()))
                        .comments(lateRequest.getExtension().getComments())
                        .responseToRequester(lateRequest.getExtension().getReviewerResponse())
                        .responseTimestamp(lateRequest.getExtension().getReviewerResponseTimestamp())
                    ) : null)
        ).toList());
    }
}

