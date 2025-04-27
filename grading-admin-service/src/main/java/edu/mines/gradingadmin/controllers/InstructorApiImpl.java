package edu.mines.gradingadmin.controllers;

import edu.mines.gradingadmin.api.InstructorApiDelegate;
import edu.mines.gradingadmin.data.*;
import edu.mines.gradingadmin.factories.DTOFactory;
import edu.mines.gradingadmin.managers.SecurityManager;
import edu.mines.gradingadmin.models.*;
import edu.mines.gradingadmin.models.tasks.ScheduledTaskDef;
import edu.mines.gradingadmin.services.*;
import jakarta.transaction.Transactional;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.context.request.NativeWebRequest;

import java.io.IOException;
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
    private final RawScoreService rawScoreService;

    public InstructorApiImpl(CourseService courseService, ExtensionService extensionService, CourseMemberService courseMemberService, SecurityManager securityManager, MigrationService migrationService, PolicyService policyService, RawScoreService rawScoreService) {
        this.courseService = courseService;
        this.extensionService = extensionService;
        this.courseMemberService = courseMemberService;
        this.securityManager = securityManager;
        this.migrationService = migrationService;
        this.policyService = policyService;
        this.rawScoreService = rawScoreService;
    }

    @Override
    public ResponseEntity<List<MigrationWithScoresDTO>> getMasterMigrationToReview(String courseId, String masterMigrationId) {
        return ResponseEntity.ok(migrationService.getMasterMigrationToReview(masterMigrationId));
    }

    @Override
    public ResponseEntity<List<TaskDTO>> applyMasterMigration(String courseId, String masterMigrationId) {
        List<ScheduledTaskDef> tasks = migrationService.startProcessScoresAndExtensions(securityManager.getUser(), masterMigrationId);

        return ResponseEntity.accepted().body(tasks.stream().map(DTOFactory::toDto).toList());
    }

    @Override
    public ResponseEntity<Void> applyValidateMasterMigration(String courseId, String masterMigrationId) {
        boolean b = migrationService.validateApplyMasterMigration(masterMigrationId);

        if (!b){
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<MasterMigrationDTO> createMasterMigration(String courseId) {
        Optional<MasterMigrationDTO> masterMigration = migrationService.createMasterMigration(courseId, securityManager.getUser()).map(DTOFactory::toDto);

        if (masterMigration.isEmpty()){
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(masterMigration.get());

    }

    @Override
    public ResponseEntity<MasterMigrationDTO> createMigrationForMasterMigration(String courseId, String masterMigrationId, String assignment) {
        Optional<MasterMigrationDTO> masterMigration = migrationService.addMigration(masterMigrationId, assignment).map(DTOFactory::toDto);

        if (masterMigration.isEmpty()){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(masterMigration.get());
    }

    @Override
    public ResponseEntity<List<ErrorResponseDTO>> getAllApprovedExtensionsForAssignment(String courseId, String assignmentId, String extensionId, String status) {
        return InstructorApiDelegate.super.getAllApprovedExtensionsForAssignment(courseId, assignmentId, extensionId, status);
    }

    @Override
    public ResponseEntity<List<ErrorResponseDTO>> getAllApprovedExtensionsForMember(String courseId, String userId, String extensionId, String status) {
        return InstructorApiDelegate.super.getAllApprovedExtensionsForMember(courseId, userId, extensionId, status);
    }

    @Override
    public ResponseEntity<List<ErrorResponseDTO>> getAllExtensionsForSection(String courseId, String sectionId, String extensionId, String status) {
        return InstructorApiDelegate.super.getAllExtensionsForSection(courseId, sectionId, extensionId, status);
    }

    @Override
    public ResponseEntity<List<MasterMigrationDTO>> getAllMigrations(String courseId) {
        List<MasterMigration> masterMigrations = migrationService.getAllMasterMigrations(courseId);

        return ResponseEntity.ok(masterMigrations.stream().map(DTOFactory::toDto).toList());
    }

    @Override
    public ResponseEntity<MasterMigrationDTO> getMasterMigrations(String courseId, String masterMigrationId) {
        Optional<MasterMigrationDTO> masterMigration = migrationService.getMasterMigration(masterMigrationId).map(DTOFactory::toDto);

        if (masterMigration.isEmpty()){
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(masterMigration.get());
    }

    @Override
    public ResponseEntity<Void> loadMasterMigration(String courseId, String masterMigrationId) {
        Optional<MasterMigration> masterMigration = migrationService.finalizeLoadMasterMigration(masterMigrationId);

        if (masterMigration.isEmpty()){
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> loadValidateMasterMigration(String courseId, String masterMigrationId) {
        boolean b = migrationService.validateLoadMasterMigration(masterMigrationId);

        if (!b) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<ExtensionDTO> newExtension(String courseId, String assignmentId, String userId, String extensionId) {
        return InstructorApiDelegate.super.newExtension(courseId, assignmentId, userId, extensionId);
    }

    @Override
    public ResponseEntity<List<TaskDTO>> postMasterMigration(String courseId, String masterMigrationId) {
        return ResponseEntity.accepted().body(migrationService.processMigrationLog(securityManager.getUser(), masterMigrationId).stream().map(DTOFactory::toDto).toList());
    }

    @Override
    public ResponseEntity<Void> reviewMasterMigration(String courseId, String masterMigrationId) {
        boolean b = migrationService.finalizeReviewMasterMigration(masterMigrationId);

        if (!b) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<MasterMigrationDTO> setPolicy(String courseId, String masterMigrationId, String migrationId, String policyId) {
        Optional<Migration> newMigration = migrationService.setPolicyForMigration(migrationId, policyId);
        if (newMigration.isEmpty()){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.accepted().build();

    }

    @Override
    public ResponseEntity<Void> updateStudentScore(String courseId, String masterMigrationId, String migrationId, MigrationScoreChangeDTO migrationScoreChangeDTO) {
        boolean b = migrationService.updateStudentScore(securityManager.getUser(), migrationId, migrationScoreChangeDTO);

        if (!b){
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.accepted().build();
    }

    @Override
    public ResponseEntity<MasterMigrationDTO> uploadRawScores(String courseId, String masterMigrationId, String migrationId, Resource body) {
        // this should be moved to tasks
        try {
            switch (migrationService.getAssignmentForMigration(migrationId).getExternalAssignmentConfig().getType()){
                case GRADESCOPE -> {
                        rawScoreService.uploadGradescopeCSV(body.getInputStream(), UUID.fromString(migrationId));
                }
                case PRAIRIELEARN -> rawScoreService.uploadPrairieLearnCSV(body.getInputStream(), UUID.fromString(migrationId));
                case RUNESTONE -> rawScoreService.uploadRunestoneCSV(body.getInputStream(), UUID.fromString(migrationId));
                default -> throw new IllegalStateException("Request to import non supported csv");
            }
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }

        Optional<MasterMigrationDTO> masterMigration = migrationService.getMasterMigration(masterMigrationId).map(DTOFactory::toDto);

        if (masterMigration.isEmpty()){
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.accepted().body(masterMigration.get());

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

    @Override
    public ResponseEntity<List<TaskDTO>> finalizeMasterMigration(String courseId, String masterMigrationId) {
        migrationService.finalizeReviewMasterMigration(masterMigrationId);

        return ResponseEntity.ok().build();
    }
}
