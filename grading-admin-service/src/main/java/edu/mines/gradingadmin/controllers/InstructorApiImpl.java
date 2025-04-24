package edu.mines.gradingadmin.controllers;

import edu.mines.gradingadmin.api.InstructorApiDelegate;
import edu.mines.gradingadmin.data.*;
import edu.mines.gradingadmin.factories.DTOFactory;
import edu.mines.gradingadmin.managers.SecurityManager;
import edu.mines.gradingadmin.models.*;
import edu.mines.gradingadmin.services.CourseMemberService;
import edu.mines.gradingadmin.services.CourseService;
import edu.mines.gradingadmin.services.ExtensionService;
import edu.mines.gradingadmin.services.MigrationService;
import edu.mines.gradingadmin.services.PolicyService;
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
    private final ExtensionService extensionService;
    private final CourseMemberService courseMemberService;
    private final SecurityManager securityManager;
    private final MigrationService migrationService;
    private final PolicyService policyService;

    public InstructorApiImpl(CourseService courseService, ExtensionService extensionService, CourseMemberService courseMemberService, SecurityManager securityManager, MigrationService migrationService, PolicyService policyService) {
        this.courseService = courseService;
        this.extensionService = extensionService;
        this.courseMemberService = courseMemberService;
        this.securityManager = securityManager;
        this.migrationService = migrationService;
        this.policyService = policyService;
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

        CourseDTO courseDTO = DTOFactory.toDto(course.get())
                .assignments(course.get().getAssignments().stream().map(DTOFactory::toDto).toList())
                .members(sections.stream().map(Section::getMembers).flatMap(Set::stream).map(DTOFactory::toDto).collect(toList()))
                .sections(sections.stream().map(Section::getName).toList());

        return ResponseEntity.ok(courseDTO);
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

        return ResponseEntity.ok(courseMemberService.searchCourseMembers(course.get(), List.of(), name, cwid)
            .stream().map(DTOFactory::toDto).toList());
    }

    @Override
    public ResponseEntity<List<MasterMigrationDTO>> getAllMasterMigrationsForCourse(String courseId){
        List<MasterMigration> masterMigrations = migrationService.getAllMasterMigrations(courseId);

        return ResponseEntity.ok(masterMigrations.stream().map(DTOFactory::toDto).toList());
    }

    @Override
    public ResponseEntity<MasterMigrationDTO> createMasterMigration(String courseId, MasterMigrationDTO masterMigrationDTO){
        Optional<MasterMigrationDTO> masterMigration = migrationService.createMasterMigration(courseId, securityManager.getUser()).map(DTOFactory::toDto);

        if (masterMigration.isEmpty()){
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(masterMigration.get());
    }

    @Override
    public ResponseEntity<MigrationDTO> updatePolicy(String courseId, String migrationId, String assignmentId, PolicyDTO policyDTO){
        Optional<Migration> newMigration = migrationService.updatePolicyForMigration(migrationId, policyDTO.getUri());
        if (newMigration.isEmpty()){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.accepted().build();
    }

    @Override
    public ResponseEntity<MasterMigrationDTO> createMigrationForMasterMigration(String courseId, String masterMigrationId, MigrationDTO migrationDTO) {
        AssignmentDTO assignment = migrationDTO.getAssignment();
        PolicyDTO policy = migrationDTO.getPolicy();
        Optional<MasterMigrationDTO> masterMigration = migrationService.addMigration(masterMigrationId, assignment.getId(), policy.getUri()).map(DTOFactory::toDto);;

        if (masterMigration.isEmpty()){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(masterMigration.get());
    }

    @Override
    public ResponseEntity<LateRequestDTO> approveExtension(String courseId, String assignmentId, String userId, String extensionId) {
        Optional<Course> course = courseService.getCourse(UUID.fromString(courseId));

        if(course.isEmpty()) {
            // need to do this with error controller
            return ResponseEntity.badRequest().build();
        }

        Optional<LateRequest> lateRequest = extensionService.approveExtension(assignmentId, userId, extensionId);

        if(lateRequest.isEmpty()) {
            // need to do this with error controller
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(DTOFactory.toDto(lateRequest.get()));
    }

    @Override
    public ResponseEntity<LateRequestDTO> denyExtension(String courseId, String assignmentId, String userId, String extensionId, String reason) {
        Optional<Course> course = courseService.getCourse(UUID.fromString(courseId));

        if(course.isEmpty()) {
            // need to do this with error controller
            return ResponseEntity.badRequest().build();
        }

        Optional<LateRequest> lateRequest = extensionService.denyExtension(assignmentId, userId, extensionId, reason);

        if(lateRequest.isEmpty()) {
            // need to do this with error controller
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(DTOFactory.toDto(lateRequest.get()));
    }

    @Override
    public ResponseEntity<List<PolicyDTO>> getAllPolicies(String courseId) {
        List<Policy> policies = policyService.getAllPolicies(UUID.fromString(courseId));
        return ResponseEntity.ok(policies.stream().map(DTOFactory::toDto).toList());
    }
}
