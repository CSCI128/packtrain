package edu.mines.gradingadmin.controllers;

import edu.mines.gradingadmin.api.StudentApiDelegate;
import edu.mines.gradingadmin.data.*;
import edu.mines.gradingadmin.factories.DTOFactory;
import edu.mines.gradingadmin.managers.SecurityManager;
import edu.mines.gradingadmin.models.*;
import edu.mines.gradingadmin.models.enums.CourseRole;
import edu.mines.gradingadmin.models.enums.LateRequestType;
import edu.mines.gradingadmin.services.*;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

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
        List<Course> courses = courseService.getCourses(true);
        return ResponseEntity.ok(courses.stream().map(DTOFactory::toSlimDto).toList());
    }

    @Override
    public ResponseEntity<StudentInformationDTO> getCourseInformationStudent(String courseId) {
        Optional<Course> course = courseService.getCourse(UUID.fromString(courseId));

        if(course.isEmpty()) {
            // need to do this with error controller
            return ResponseEntity.badRequest().build();
        }

        Optional<CourseMember> courseMember = courseMemberService.getCourseMemberByCourseByCwid(course.get(), securityManager.getUser().getCwid());
        if(courseMember.isEmpty()) {
            // need to do this with error controller
            return ResponseEntity.badRequest().build();
        }

        Set<Section> sections = courseMemberService.getSectionsForUserAndCourse(securityManager.getUser(), course.get());
        CourseRole courseRole = courseMemberService.getRoleForUserAndCourse(securityManager.getUser(), course.get().getId());

        CourseDTO courseDTO = DTOFactory.toDto(course.get())
            .assignments(course.get().getAssignments().stream()
                    .filter(Assignment::isEnabled)
                    .filter(assignment -> assignment.getDueDate() != null && assignment.getDueDate().isAfter(Instant.now())).map(DTOFactory::toDto).toList())
            .sections(sections.stream().map(Section::getName).toList());

        StudentInformationDTO studentInformationDTO = new StudentInformationDTO()
                .course(courseDTO)
                .latePassesUsed(courseMember.get().getLatePassesUsed())
                .courseRole(StudentInformationDTO.CourseRoleEnum.fromValue(courseRole.getRole()));

        Optional<Section> section = sections.stream().findFirst();
        if(section.isPresent()) {
            Optional<CourseMember> instructor = courseMemberService.getFirstSectionInstructor(section.get());
            if(instructor.isEmpty()) {
                studentInformationDTO.setProfessor("");
                log.error("Could not find professor for section: {}", section.get().getId());
            }
            else {
                studentInformationDTO.setProfessor(instructor.get().getUser().getName());
            }
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

        Optional<Course> course = courseService.getCourse(UUID.fromString(courseId));
        if(course.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        LateRequest lateRequest = extensionService.createLateRequest(
            courseId,
            user,
            lateRequestDTO.getRequestType(),
            lateRequestDTO.getNumDaysRequested(),
            lateRequestDTO.getDateSubmitted(),
            lateRequestDTO.getAssignmentId(),
            lateRequestDTO.getStatus(),
            lateRequestDTO.getExtension());

        if(lateRequest.getLateRequestType() == LateRequestType.LATE_PASS) {
            courseMemberService.useLatePasses(course.get(), user, lateRequestDTO.getNumDaysRequested());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(DTOFactory.toDto(lateRequest));
    }

    @Override
    public ResponseEntity<Void> withdrawExtension(String courseId, String extensionId) {
        Optional<LateRequest> lateRequest = extensionService.getLateRequest(UUID.fromString(extensionId));
        Optional<Course> course = courseService.getCourse(UUID.fromString(courseId));

        if(lateRequest.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        if(course.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        if(lateRequest.get().getLateRequestType() == LateRequestType.LATE_PASS) {
            courseMemberService.refundLatePasses(course.get(), securityManager.getUser(), lateRequest.get().getDaysRequested());
        }
        extensionService.deleteLateRequest(lateRequest.get());
        return ResponseEntity.noContent().build();
    }
}

