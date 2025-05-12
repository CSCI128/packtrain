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
import org.springframework.web.server.ResponseStatusException;

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
        MasterMigration masterMigration = migrationService.createMasterMigration(courseId, securityManager.getUser());

        return ResponseEntity.status(HttpStatus.CREATED).body(DTOFactory.toDto(masterMigration));

    }

    @Override
    public ResponseEntity<Void> deleteMasterMigration(String courseId, String masterMigrationId) {
        if (!migrationService.deleteMasterMigration(UUID.fromString(courseId), UUID.fromString(masterMigrationId))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to delete master migration!");
        }

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Override
    public ResponseEntity<MasterMigrationDTO> createMigrationForMasterMigration(String courseId, String masterMigrationId, String assignment) {
        MasterMigration masterMigration = migrationService.addMigration(masterMigrationId, assignment);

        return ResponseEntity.status(HttpStatus.CREATED).body(DTOFactory.toDto(masterMigration));
    }

    @Override
    public ResponseEntity<List<LateRequestDTO>> getAllExtensionsForCourse(String courseId, String status) {
        List<LateRequest> lateRequests = extensionService.getAllLateRequests(courseId, status);

        return ResponseEntity.ok(lateRequests.stream().map(DTOFactory::toDto).toList());
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
        MasterMigration masterMigration = migrationService.getMasterMigration(masterMigrationId);

        return ResponseEntity.ok(DTOFactory.toDto(masterMigration));
    }

    @Override
    public ResponseEntity<Void> loadMasterMigration(String courseId, String masterMigrationId) {
        boolean res =  migrationService.finalizeLoadMasterMigration(masterMigrationId);

        if(!res) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to finalize load phase for migration!");
        }

        return ResponseEntity.accepted().build();
    }

    @Override
    public ResponseEntity<Void> loadValidateMasterMigration(String courseId, String masterMigrationId) {
        boolean res = migrationService.validateLoadMasterMigration(masterMigrationId);

        if (!res) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to validate load phase for migration!");
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
        boolean res = migrationService.finalizeReviewMasterMigration(masterMigrationId);

        if (!res) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to finalize review phase for migration!");
        }

        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<MasterMigrationDTO> setPolicy(String courseId, String masterMigrationId, String migrationId, String policyId) {
        Migration newMigration = migrationService.setPolicyForMigration(migrationId, policyId);

        MasterMigration masterMigration = migrationService.getMasterMigration(masterMigrationId);

        return ResponseEntity.accepted().body(DTOFactory.toDto(masterMigration));
    }

    @Override
    public ResponseEntity<Void> updateStudentScore(String courseId, String masterMigrationId, String migrationId, MigrationScoreChangeDTO migrationScoreChangeDTO) {
        boolean res = migrationService.updateStudentScore(securityManager.getUser(), migrationId, migrationScoreChangeDTO);

        if (!res){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to update score for student!");
        }

        return ResponseEntity.accepted().build();
    }

    @Override
    public ResponseEntity<MasterMigrationDTO> uploadRawScores(String courseId, String masterMigrationId, String migrationId, Resource body) {
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

        MasterMigration masterMigration = migrationService.getMasterMigration(masterMigrationId);

        return ResponseEntity.accepted().body(DTOFactory.toDto(masterMigration));
    }

    @Override
    @Transactional
    public ResponseEntity<CourseDTO> getCourseInformationInstructor(String courseId) {
        Course course = courseService.getCourse(UUID.fromString(courseId));


        Set<Section> sections = courseMemberService.getSectionsForUserAndCourse(securityManager.getUser(), course);

        CourseDTO courseDTO = DTOFactory.toDto(course)
                .assignments(course.getAssignments().stream().map(DTOFactory::toDto).toList())
                .members(sections.stream().map(Section::getMembers).flatMap(Set::stream).map(DTOFactory::toDto).collect(toList()))
                .sections(sections.stream().map(Section::getName).toList());

        return ResponseEntity.ok(courseDTO);
    }

    @Override
    public ResponseEntity<List<CourseMemberDTO>> getInstructorEnrollments(String courseId, String name, String cwid) {
        Course course = courseService.getCourse(UUID.fromString(courseId));

        if(name != null && cwid != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Must define one of 'name' or 'cwid'");
        }

        return ResponseEntity.ok(courseMemberService.searchCourseMembers(course, List.of(), name, cwid)
            .stream().map(DTOFactory::toDto).toList());
    }

    @Override
    public ResponseEntity<LateRequestDTO> approveExtension(String courseId, String assignmentId, String userId, String extensionId, String reason) {
        LateRequest lateRequest = extensionService.approveExtension(assignmentId, userId, extensionId, reason);

        return ResponseEntity.accepted().body(DTOFactory.toDto(lateRequest));
    }

    @Override
    public ResponseEntity<LateRequestDTO> denyExtension(String courseId, String assignmentId, String userId, String extensionId, String reason) {
        LateRequest lateRequest = extensionService.denyExtension(assignmentId, userId, extensionId, reason);

        return ResponseEntity.accepted().body(DTOFactory.toDto(lateRequest));
    }

    @Override
    public ResponseEntity<List<PolicyDTO>> getAllPolicies(String courseId) {
        List<Policy> policies = policyService.getAllPolicies(UUID.fromString(courseId));
        return ResponseEntity.ok(policies.stream().map(DTOFactory::toDto).toList());
    }

    @Override
    public ResponseEntity<Void> finalizeMasterMigration(String courseId, String masterMigrationId) {
        boolean res = migrationService.finalizePostToCanvas(masterMigrationId);

        if (!res){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to finalize migration!");
        }

        return ResponseEntity.accepted().build();
    }
}
