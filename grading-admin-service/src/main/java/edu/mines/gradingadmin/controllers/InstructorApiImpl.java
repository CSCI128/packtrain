package edu.mines.gradingadmin.controllers;

import edu.mines.gradingadmin.api.InstructorApiDelegate;
import edu.mines.gradingadmin.data.AssignmentDTO;
import edu.mines.gradingadmin.data.CourseDTO;
import edu.mines.gradingadmin.data.CourseMemberDTO;
import edu.mines.gradingadmin.data.PolicyDTO;
import edu.mines.gradingadmin.managers.SecurityManager;
import edu.mines.gradingadmin.models.Course;
import edu.mines.gradingadmin.models.CourseMember;
import edu.mines.gradingadmin.models.Policy;
import edu.mines.gradingadmin.models.Section;
import edu.mines.gradingadmin.services.CourseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static java.util.stream.Collectors.toList;

@Controller
public class InstructorApiImpl implements InstructorApiDelegate {
    private final CourseService courseService;
    private final SecurityManager securityManager;

    public InstructorApiImpl(CourseService courseService, SecurityManager securityManager) {
        this.courseService = courseService;
        this.securityManager = securityManager;
    }

    @Override
    public ResponseEntity<CourseDTO> getCourseInformationInstructor(String courseId) {
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
                .assignments(course.get().getAssignments().stream().map(assignment ->
                    new AssignmentDTO()
                            .category(assignment.getCategory())
                            .dueDate(assignment.getDueDate())
                            .unlockDate(assignment.getUnlockDate())
                            .enabled(assignment.isEnabled())
                            .points(assignment.getPoints())).toList())
                .members(courseMember.get().getSections().stream().map(Section::getMembers).flatMap(Set::stream).map(member ->
                    new CourseMemberDTO()
                        .canvasId(member.getCanvasId())
                        .courseRole(CourseMemberDTO.CourseRoleEnum.fromValue(member.getRole().getRole()))
                        .cwid(member.getUser().getCwid())
                        .sections(member.getSections().stream().map(Section::getName).toList()))
                    .collect(toList()))
                .sections(courseMember.get().getSections().stream().map(Section::getName).toList());

        return ResponseEntity.ok(courseDTO);
    }

    @Override
    public ResponseEntity<PolicyDTO> newPolicy(String courseId, String name, String filePath, MultipartFile fileData) {
        Optional<Policy> policy = courseService.createNewCourseWidePolicy(securityManager.getUser(), UUID.fromString(courseId), name, filePath, fileData);

        if (policy.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(policy.map(p -> new PolicyDTO()
                .course(new CourseDTO()
                        .id(p.getCourse().getId().toString())
                        .name(p.getCourse().getName())
                        .code(p.getCourse().getCode())
                )
                .name(p.getPolicyName())
                .uri(p.getPolicyURI())
        ).get());


    }

    @Override
    public ResponseEntity<List<PolicyDTO>> getAllPolicies(String courseId) {
        List<Policy> policies = courseService.getAllPolicies(UUID.fromString(courseId));
        return ResponseEntity.ok(policies.stream().map(p -> new PolicyDTO()
                .course(new CourseDTO()
                        .id(p.getCourse().getId().toString())
                        .name(p.getCourse().getName())
                        .code(p.getCourse().getCode())
                )
                .name(p.getPolicyName())
                .uri(p.getPolicyURI())).toList());
    }
}
