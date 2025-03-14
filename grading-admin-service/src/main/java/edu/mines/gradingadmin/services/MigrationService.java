package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.data.messages.ScoredDTO;
import edu.mines.gradingadmin.events.NewTaskEvent;
import edu.mines.gradingadmin.models.*;
import edu.mines.gradingadmin.models.tasks.ProcessScoresAndExtensionsTaskDef;
import edu.mines.gradingadmin.models.tasks.ScheduledTaskDef;
import edu.mines.gradingadmin.repositories.MasterMigrationRepo;
import edu.mines.gradingadmin.repositories.MigrationRepo;
import edu.mines.gradingadmin.repositories.MigrationTransactionLogRepo;
import edu.mines.gradingadmin.repositories.ScheduledTaskRepo;
import edu.mines.gradingadmin.services.external.PolicyServerService;
import edu.mines.gradingadmin.services.external.RabbitMqService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
public class MigrationService {
    private final MigrationRepo migrationRepo;
    private final MasterMigrationRepo masterMigrationRepo;
    private final MigrationTransactionLogRepo transactionLogRepo;
    private final ScheduledTaskRepo<ProcessScoresAndExtensionsTaskDef> taskRepo;
    private final ExtensionService extensionService;
    private final CourseService courseService;
    private final AssignmentService assignmentService;
    private final ApplicationEventPublisher eventPublisher;
    private final RabbitMqService rabbitMqService;
    private final PolicyServerService policyServerService;

    public MigrationService(MigrationRepo migrationRepo, MasterMigrationRepo masterMigrationRepo, MigrationTransactionLogRepo transactionLogRepo, ScheduledTaskRepo<ProcessScoresAndExtensionsTaskDef> taskRepo, ExtensionService extensionService, CourseService courseService, AssignmentService assignmentService, ApplicationEventPublisher eventPublisher, RabbitMqService rabbitMqService, PolicyServerService policyServerService){
        this.migrationRepo = migrationRepo;
        this.masterMigrationRepo = masterMigrationRepo;
        this.transactionLogRepo = transactionLogRepo;
        this.taskRepo = taskRepo;
        this.extensionService = extensionService;
        this.courseService = courseService;
        this.assignmentService = assignmentService;
        this.eventPublisher = eventPublisher;
        this.rabbitMqService = rabbitMqService;
        this.policyServerService = policyServerService;
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

    public void handleScoreReceived(User asUser, UUID migrationId, ScoredDTO dto){
        MigrationTransactionLog entry = new MigrationTransactionLog();
        entry.setPerformedByUser(asUser);
        entry.setCwid(dto.getCwid());
        entry.setMigrationId(migrationId);
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
    public Optional<MasterMigration> addMigration(String masterMigrationId, String assignmentId, String policyURI){
        MasterMigration masterMigration = masterMigrationRepo.getMasterMigrationByMasterMigrationId(UUID.fromString(masterMigrationId));
        Migration migration = new Migration();
        URI uri;

        try {
            uri = new URI(policyURI);

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

    public Optional<Migration> updatePolicyForMigration(String migrationId, String policyURI){
        Migration updatedMigration = migrationRepo.getMigrationById(UUID.fromString(migrationId));
        URI uri;

        try {
            uri = new URI(policyURI);

        } catch (URISyntaxException e){
            return Optional.empty();
        }

        Optional<Policy> policy = courseService.getPolicy(uri);
        if (policy.isEmpty()){
            return Optional.empty();
        }

        updatedMigration.setPolicy(policy.get());
        return Optional.of(migrationRepo.save(updatedMigration));

    }


    public void processScoresAndExtensionsTask(ProcessScoresAndExtensionsTaskDef task){
        Optional<Assignment> assignment = assignmentService.getAssignmentById(task.getAssignmentId().toString());

        if (assignment.isEmpty()){
            throw new RuntimeException(String.format("Failed to get assignment '%s'", task.getAssignmentId()));
        }
        RabbitMqService.MigrationConfig config;

        try {
           config = rabbitMqService.createMigrationConfig(task.getMigrationId())
                    .forAssignment(assignment.get())
                    .withPolicy(task.getPolicy())
                    .withOnScoreReceived(dto -> this.handleScoreReceived(task.getCreatedByUser(), task.getMigrationId(), dto))
                    .build();
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }

        policyServerService.startGrading(config.getGradingStartDTO());

        // todo: get raw scores
        // todo: send raw scores
    }

    @Transactional
    public List<ScheduledTaskDef> startMigration(User actingUser, String masterMigrationId ){
        MasterMigration master = masterMigrationRepo.getMasterMigrationByMasterMigrationId(UUID.fromString(masterMigrationId));

        if (master.getStatus() != MigrationStatus.CREATED){
            log.warn("Migration is in invalid state to start a migration. {} != {}", master.getStatus().name(), MigrationStatus.CREATED.name());
            return List.of();
        }

        List<Migration> migrations = migrationRepo.getMigrationListByMasterMigrationId(UUID.fromString(masterMigrationId));

        log.info("Starting migration for {} assignments", migrations.size());

        List<ScheduledTaskDef> tasks = new LinkedList<>();

        for (Migration migration : migrations){
            Assignment assignment = migration.getAssignment();

            ProcessScoresAndExtensionsTaskDef task = new ProcessScoresAndExtensionsTaskDef();
            task.setCreatedByUser(actingUser);
            task.setTaskName(String.format("Process scores and extensions for assignment '%s'", assignment.getName()));
            task.setMigrationId(migration.getId());
            task.setAssignmentId(migration.getId());
            task.setPolicy(URI.create(migration.getPolicy().getPolicyURI()));
            task = taskRepo.save(task);

            tasks.add(task);

            NewTaskEvent.TaskData<ProcessScoresAndExtensionsTaskDef> taskDefinition = new NewTaskEvent.TaskData<>(taskRepo, task.getId(), this::processScoresAndExtensionsTask);

            eventPublisher.publishEvent(new NewTaskEvent(this, taskDefinition));
        }

        return tasks;
    }

}
