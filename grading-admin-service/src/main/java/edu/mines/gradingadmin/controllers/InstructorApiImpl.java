package edu.mines.gradingadmin.controllers;

import edu.mines.gradingadmin.api.InstructorApiDelegate;
import edu.mines.gradingadmin.data.*;
import edu.mines.gradingadmin.managers.SecurityManager;
import edu.mines.gradingadmin.models.*;
import edu.mines.gradingadmin.services.CourseMemberService;
import edu.mines.gradingadmin.services.CourseService;
import edu.mines.gradingadmin.services.MigrationService;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

import static java.util.stream.Collectors.toList;

@Controller
public class InstructorApiImpl implements InstructorApiDelegate {
    private final CourseService courseService;
    private final CourseMemberService courseMemberService;
    private final SecurityManager securityManager;
    private final MigrationService migrationService;

    public InstructorApiImpl(CourseService courseService, CourseMemberService courseMemberService, SecurityManager securityManager, MigrationService migrationService) {
        this.courseService = courseService;
        this.courseMemberService = courseMemberService;
        this.securityManager = securityManager;
        this.migrationService = migrationService;
    }

    @Override
    @Transactional
    public ResponseEntity<CourseDTO> getCourseInformationInstructor(String courseId) {
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
                .assignments(course.get().getAssignments().stream().map(assignment ->
                    new AssignmentDTO()
                            .category(assignment.getCategory())
                            .dueDate(assignment.getDueDate())
                            .unlockDate(assignment.getUnlockDate())
                            .enabled(assignment.isEnabled())
                            .points(assignment.getPoints())).toList())
                .members(sections.stream().map(Section::getMembers).flatMap(Set::stream).map(member ->
                    new CourseMemberDTO()
                        .canvasId(member.getCanvasId())
                        .courseRole(CourseMemberDTO.CourseRoleEnum.fromValue(member.getRole().getRole()))
                        .cwid(member.getUser().getCwid())
                        .sections(member.getSections().stream().map(Section::getName).toList()))
                    .collect(toList()))
                .sections(sections.stream().map(Section::getName).toList());

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

    @Override
    public ResponseEntity<List<CourseMemberDTO>> getInstructorEnrollments(String courseId, String name, String cwid) {
        Optional<Course> course = courseService.getCourse(UUID.fromString(courseId));

        if(course.isEmpty()) {
            // need to do this with error controller
            return ResponseEntity.badRequest().build();
        }

        if(name != null && cwid != null) {
            // need to do this with error controller
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(courseMemberService.searchCourseMembers(course.get(), List.of(), name, cwid).stream()
                .map(member -> new CourseMemberDTO()
                        .canvasId(member.getCanvasId())
                        .courseRole(CourseMemberDTO.CourseRoleEnum.fromValue(member.getRole().getRole()))
                        .cwid(member.getUser().getCwid())
                        .sections(member.getSections().stream().map(Section::getName).toList()))
                .toList());
    }

    @Override
    public ResponseEntity<List<MasterMigrationDTO>> getAllMasterMigrationsForCourse(String courseId){
        List<MasterMigration> masterMigrations = migrationService.getAllMasterMigrations(courseId);
        return ResponseEntity.ok(masterMigrations.stream().map(
                mastermigration -> new MasterMigrationDTO()
                        .migrationList( mastermigration.getMigrations().stream().map(
                                migration -> new MigrationDTO()
                                        .policy( new PolicyDTO().uri(migration.getPolicy().getPolicyURI()))
                                        .assignment(new AssignmentDTO().id(migration.getAssignment().getId().toString())
                                                .name(migration.getAssignment().getName()))).toList()))
                .toList());
    }


}
