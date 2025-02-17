package edu.mines.gradingadmin.controllers;

import edu.mines.gradingadmin.api.StudentApiDelegate;
import edu.mines.gradingadmin.data.AssignmentDTO;
import edu.mines.gradingadmin.data.CourseDTO;
import edu.mines.gradingadmin.managers.SecurityManager;
import edu.mines.gradingadmin.models.Assignment;
import edu.mines.gradingadmin.models.Course;
import edu.mines.gradingadmin.models.Section;
import edu.mines.gradingadmin.models.User;
import edu.mines.gradingadmin.services.AssignmentService;
import edu.mines.gradingadmin.services.CourseMemberService;
import edu.mines.gradingadmin.services.CourseService;
import edu.mines.gradingadmin.services.SectionService;
import edu.mines.gradingadmin.services.UserService;
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
    private final CourseMemberService courseMemberService;
    private final AssignmentService assignmentService;
    private final SecurityManager securityManager;

    public StudentApiImpl(UserService userService, CourseService courseService, SectionService sectionService, CourseMemberService courseMemberService, AssignmentService assignmentService, SecurityManager securityManager) {
        this.userService = userService;
        this.courseService = courseService;
        this.sectionService = sectionService;
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
    public ResponseEntity<CourseDTO> getCourseInformationStudent(String courseId) {
        Optional<Course> course = courseService.getCourse(UUID.fromString(courseId));

        if(course.isEmpty()) {
            // need to do this with error controller
            return ResponseEntity.badRequest().build();
        }

        Set<Section> sections = courseMemberService.getSectionsForUserAndCourse(securityManager.getUser(), course.get());

        CourseDTO courseDTO = new CourseDTO()
            .id(course.get().getId().toString())
            .code(course.get().getCode())
            .name(course.get().getName())
            .term(course.get().getTerm())
            .enabled(course.get().isEnabled())
            .canvasId(course.get().getCanvasId())
            .sections(sections.stream().map(Section::getName).toList());

        return ResponseEntity.ok(courseDTO);
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
}

