package edu.mines.packtrain.controllers;

import edu.mines.packtrain.api.StudentApiDelegate;
import edu.mines.packtrain.data.*;
import edu.mines.packtrain.factories.DTOFactory;
import edu.mines.packtrain.managers.SecurityManager;
import edu.mines.packtrain.models.*;
import edu.mines.packtrain.models.enums.CourseRole;
import edu.mines.packtrain.models.enums.LateRequestType;
import edu.mines.packtrain.services.*;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

@Transactional
@Controller
@Slf4j
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
    public ResponseEntity<List<CourseSlimDTO>> getCoursesStudent() {
        User user = securityManager.getUser();
        List<Course> courses = courseService.getCoursesStudent(user);
        return ResponseEntity.ok(courses.stream().map(DTOFactory::toSlimDto).toList());
    }

    @Override
    public ResponseEntity<StudentInformationDTO> getCourseInformationStudent(String courseId) {
        Course course = courseService.getCourse(UUID.fromString(courseId));

        Optional<CourseMember> courseMember = courseMemberService.findCourseMemberGivenCourseAndCwid(course, securityManager.getUser().getCwid());

        if(courseMember.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Student is not enrolled in course!");
        }

        Set<Section> sections = courseMemberService.getSectionsForUserAndCourse(securityManager.getUser(), course);
        CourseRole courseRole = courseMemberService.getRoleForUserAndCourse(securityManager.getUser(), course.getId());

        CourseDTO courseDTO = DTOFactory.toDto(course)
            .assignments(course.getAssignments().stream()
                    .filter(Assignment::isEnabled)
                    .filter(assignment -> assignment.getDueDate() != null && assignment.getDueDate().isAfter(Instant.now())).map(DTOFactory::toDto).toList())
            .sections(sections.stream().map(Section::getName).toList());

        StudentInformationDTO studentInformationDTO = new StudentInformationDTO()
                .course(courseDTO)
                .latePassesUsed(courseMember.get().getLatePassesUsed())
                .courseRole(StudentInformationDTO.CourseRoleEnum.fromValue(courseRole.getRole()));

        Optional<Section> section = sections.stream().findFirst();
        if(section.isPresent()) {
            CourseMember instructor = courseMemberService.getStudentInstructor(course, section);

            studentInformationDTO.setProfessor(instructor.getUser().getName());
        }

        return ResponseEntity.ok(studentInformationDTO);
    }

    @Override
    public ResponseEntity<List<AssignmentSlimDTO>> getCourseAssignmentsStudent(String courseId) {
        List<Assignment> assignments = assignmentService.getAllUnlockedAssignments(courseId);
        return ResponseEntity.ok(assignments.stream().map(DTOFactory::toSlimDto).toList());
    }

    @Override
    public ResponseEntity<List<LateRequestDTO>> getAllExtensions(String courseId) {
        User user = securityManager.getUser();

        List<LateRequest> lateRequests = extensionService.getAllLateRequestsForStudent(courseId, user);

        return ResponseEntity.ok(lateRequests.stream().map(DTOFactory::toDto).toList());
    }

    @Override
    public ResponseEntity<LateRequestDTO> createExtensionRequest(String courseId, LateRequestDTO lateRequestDTO) {
        User user = securityManager.getUser();

        Course course = courseService.getCourse(UUID.fromString(courseId));

        LateRequest lateRequest = extensionService.createLateRequest(
            courseId,
            user,
            lateRequestDTO.getRequestType(),
            lateRequestDTO.getNumDaysRequested(),
            lateRequestDTO.getDateSubmitted(),
            lateRequestDTO.getAssignmentId(),
            lateRequestDTO.getExtension()
        );

        if(lateRequest.getLateRequestType() == LateRequestType.LATE_PASS) {
            courseMemberService.useLatePasses(course, user, lateRequestDTO.getNumDaysRequested());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(DTOFactory.toDto(lateRequest));
    }

    @Override
    public ResponseEntity<Void> withdrawExtension(String courseId, String extensionId) {
        Course course = courseService.getCourse(UUID.fromString(courseId));

        extensionService.deleteLateRequest(course, securityManager.getUser(), extensionId);

        return ResponseEntity.noContent().build();
    }
}

