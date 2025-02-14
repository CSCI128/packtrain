package edu.mines.gradingadmin.controllers;

import edu.mines.gradingadmin.api.InstructorApiDelegate;
import edu.mines.gradingadmin.data.CourseDTO;
import edu.mines.gradingadmin.data.PolicyDTO;
import edu.mines.gradingadmin.managers.SecurityManager;
import edu.mines.gradingadmin.models.Policy;
import edu.mines.gradingadmin.services.CourseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
public class InstructorApiImpl implements InstructorApiDelegate {
    private final CourseService courseService;
    private final SecurityManager securityManager;

    public InstructorApiImpl(CourseService courseService, SecurityManager securityManager) {
        this.courseService = courseService;
        this.securityManager = securityManager;
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
