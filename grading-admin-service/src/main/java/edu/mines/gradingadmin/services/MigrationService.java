package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.data.AssignmentDTO;
import edu.mines.gradingadmin.data.PolicyDTO;
import edu.mines.gradingadmin.data.messages.ScoredDTO;
import edu.mines.gradingadmin.models.*;
import edu.mines.gradingadmin.repositories.MasterMigrationRepo;
import edu.mines.gradingadmin.repositories.MigrationRepo;
import edu.mines.gradingadmin.repositories.MigrationTransactionLogRepo;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.C;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class MigrationService {
    private final MigrationRepo migrationRepo;
    private final MasterMigrationRepo masterMigrationRepo;
    private final MigrationTransactionLogRepo transactionLogRepo;
    private final ExtensionService extensionService;
    private final CourseService courseService;
    private final AssignmentService assignmentService;

    public MigrationService(MigrationRepo migrationRepo, MasterMigrationRepo masterMigrationRepo, MigrationTransactionLogRepo transactionLogRepo, ExtensionService extensionService, CourseService courseService, AssignmentService assignmentService){
        this.migrationRepo = migrationRepo;
        this.masterMigrationRepo = masterMigrationRepo;
        this.transactionLogRepo = transactionLogRepo;
        this.extensionService = extensionService;
        this.courseService = courseService;
        this.assignmentService = assignmentService;
    }

    public MasterMigration createMigrationForAssignments(Course course, List<Policy> policyList, List<Assignment> assignmentList){
        MasterMigration masterMigration = new MasterMigration();
        masterMigration.setCourse(course);
        List<Migration> migrations = new ArrayList<>();
        for (int i = 0; i < assignmentList.size(); i++){
            Migration migration = new Migration();
            migration.setAssignment(assignmentList.get(i));
            migration.setPolicy(policyList.get(i));
            migrations.add(migrationRepo.save(migration));
        }
        masterMigration.setMigrations(migrations);
        return masterMigrationRepo.save(masterMigration);
    }

    @Transactional
    public List<MasterMigration> getAllMasterMigrations(String courseId){
       return masterMigrationRepo.getMasterMigrationsByCourseId(UUID.fromString(courseId));

    }

    public void handleScoreReceived(User asUser, Migration migration, ScoredDTO dto){
        MigrationTransactionLog entry = new MigrationTransactionLog();
        entry.setPerformedByUser(asUser);
        entry.setCwid(dto.getCwid());
        entry.setMigrationId(migration.getId());
        entry.setExtensionId(dto.getExtensionId());
        if (entry.getExtensionId() != null){
            entry.setExtensionApplied(dto.getExtensionStatus().equals(ScoredDTO.ExtensionStatus.APPLIED));
        }
        else {
            entry.setExtensionApplied(false);
        }
        entry.setSubmissionStatus(SubmissionStatus.valueOf(dto.getSubmissionStatus().toString()));
        entry.setScore(dto.getFinalScore());
        entry.setSubmissionTime(dto.getAdjustedSubmissionTime());

        StringBuilder msg = new StringBuilder();
        Optional<LateRequest> lateRequest = extensionService.getLateRequest(entry.getExtensionId());

        if (entry.isExtensionApplied() && lateRequest.isPresent()){
            msg.append(String.format("Applied extension '%s' submitted on '%s' for %d days\n\n", lateRequest.get().getRequestType(), lateRequest.get().getSubmissionDate().toString(), lateRequest.get().getDaysRequested()));
        }

        if (lateRequest.isPresent() && lateRequest.get().getExtension() != null && lateRequest.get().getExtension().getReviewerResponse() != null && !lateRequest.get().getExtension().getReviewerResponse().isEmpty()){
            msg.append(String.format("Reviewer Response: %s\n\n", lateRequest.get().getExtension().getReviewerResponse()));
        }

        if (dto.getExtensionMessage() != null && !dto.getExtensionMessage().isEmpty()){
            msg.append(String.format("Generated Extension Message: %s\n\n", dto.getExtensionMessage()));
        }

        if (dto.getSubmissionMessage() != null && !dto.getSubmissionMessage().isEmpty()){
            msg.append(String.format("Generated Submission Message: %s\n\n", dto.getSubmissionMessage()));
        }

        if (!msg.isEmpty()){
            entry.setMessage(msg.toString());
        }

        transactionLogRepo.save(entry);
    }

    public Optional<MasterMigration> createMasterMigration(String courseId){
        MasterMigration masterMigration = new MasterMigration();
        Optional<Course> course = courseService.getCourse(UUID.fromString(courseId));
        if (course.isEmpty()) {
            return Optional.empty();
        }
        masterMigration.setCourse(course.get());
        masterMigrationRepo.save(masterMigration);
        return Optional.of(masterMigration);
    }

    @Transactional
    public Optional<MasterMigration> addMigration(String masterMigrationId, String assignmentId, String policyUri){
        MasterMigration masterMigration = masterMigrationRepo.getMasterMigrationByMasterMigrationId(UUID.fromString(masterMigrationId));
        Migration migration = new Migration();
        URI uri;

        try {
            uri = new URI(policyUri);

        } catch (URISyntaxException e){
            return Optional.empty();
        }
        Optional<Policy> policy = courseService.getPolicy(uri);
        if (policy.isEmpty()){
            return Optional.empty();
        }
        migration.setPolicy(policy.get());
        Optional<Assignment> assignment = assignmentService.getAssignmentById(assignmentId);
       if (assignment.isEmpty()){
           return Optional.empty();
       }
        migration.setAssignment(assignment.get());
        List<Migration> migrationList = masterMigration.getMigrations();
        migrationList.add(migration);
        masterMigration.setMigrations(migrationList);
        migration.setMasterMigration(masterMigration);
        migrationRepo.save(migration);
        masterMigrationRepo.save(masterMigration);
        return Optional.of(masterMigration);
    }

    public List<Migration> getMigrationsByMasterMigration(String masterMigrationId){
        return migrationRepo.getMigrationListByMasterMigrationId(UUID.fromString(masterMigrationId));
    }

    public Migration getMigration(String migrationId){
        return migrationRepo.getMigrationById(UUID.fromString(migrationId));
    }

    public Optional<Migration> updatePolicyForMigration(String migrationId, Policy newPolicy){
        Migration updatedMigration = migrationRepo.getMigrationById(UUID.fromString(migrationId));
        updatedMigration.setPolicy(newPolicy);
        migrationRepo.save(updatedMigration);
        return Optional.of(updatedMigration);
    }

}
