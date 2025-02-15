package edu.mines.gradingadmin.controllers;

import edu.mines.gradingadmin.api.StudentApiDelegate;
import edu.mines.gradingadmin.data.CourseDTO;
import edu.mines.gradingadmin.managers.SecurityManager;
import edu.mines.gradingadmin.models.Course;
import edu.mines.gradingadmin.models.CourseMember;
import edu.mines.gradingadmin.models.Section;
import edu.mines.gradingadmin.services.CourseMemberService;
import edu.mines.gradingadmin.services.CourseService;
import edu.mines.gradingadmin.services.SectionService;
import jakarta.transaction.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Transactional
@Controller
public class StudentApiImpl implements StudentApiDelegate {

    private final CourseService courseService;
    private final SectionService sectionService;
    private final CourseMemberService courseMemberService;
    private final SecurityManager securityManager;

    public StudentApiImpl(CourseService courseService, SectionService sectionService, CourseMemberService courseMemberService, SecurityManager securityManager) {
        this.courseService = courseService;
        this.sectionService = sectionService;
        this.courseMemberService = courseMemberService;
        this.securityManager = securityManager;
    }

    @Override
    public ResponseEntity<CourseDTO> getCourseInformationStudent(String courseId) {
        Optional<Course> course = courseService.getCourse(UUID.fromString(courseId));

        if(course.isEmpty()) {
            // need to do this with error controller
            return ResponseEntity.badRequest().build();
        }

        Optional<CourseMember> courseMember = securityManager.getUser().getCourseMemberships().stream().filter(c -> c.getId().toString().equals(courseId)).findFirst();
        if(courseMember.isEmpty()) {
            // need to do this with error controller
            return ResponseEntity.badRequest().build();
        }

        CourseDTO courseDTO = new CourseDTO()
            .id(course.get().getId().toString())
            .code(course.get().getCode())
            .name(course.get().getName())
            .term(course.get().getTerm())
            .enabled(course.get().isEnabled())
            .canvasId(course.get().getCanvasId())
            .sections(courseMember.get().getSections().stream().map(Section::getName).toList());

        return ResponseEntity.ok(courseDTO);
    }
}

