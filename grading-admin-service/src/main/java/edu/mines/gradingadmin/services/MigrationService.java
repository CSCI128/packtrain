package edu.mines.gradingadmin.services;

import edu.ksu.canvas.model.Progress;
import edu.mines.gradingadmin.data.AssignmentSlimDTO;
import edu.mines.gradingadmin.data.MigrationScoreChangeDTO;
import edu.mines.gradingadmin.data.MigrationWithScoresDTO;
import edu.mines.gradingadmin.data.ScoreDTO;
import edu.mines.gradingadmin.data.policyServer.RawGradeDTO;
import edu.mines.gradingadmin.data.policyServer.ScoredDTO;
import edu.mines.gradingadmin.events.NewTaskEvent;
import edu.mines.gradingadmin.factories.DTOFactory;
import edu.mines.gradingadmin.factories.MigrationFactory;
import edu.mines.gradingadmin.managers.IdentityProvider;
import edu.mines.gradingadmin.managers.ImpersonationManager;
import edu.mines.gradingadmin.models.*;
import edu.mines.gradingadmin.models.enums.*;
import edu.mines.gradingadmin.models.tasks.PostToCanvasTaskDef;
import edu.mines.gradingadmin.models.tasks.ProcessScoresAndExtensionsTaskDef;
import edu.mines.gradingadmin.models.tasks.ScheduledTaskDef;
import edu.mines.gradingadmin.models.tasks.ZeroOutSubmissionsTaskDef;
import edu.mines.gradingadmin.repositories.*;
import edu.mines.gradingadmin.services.external.CanvasService;
import edu.mines.gradingadmin.services.external.PolicyServerService;
import edu.mines.gradingadmin.services.external.RabbitMqService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
public class MigrationService {
    private final MigrationRepo migrationRepo;
    private final MasterMigrationRepo masterMigrationRepo;
    private final MigrationTransactionLogRepo transactionLogRepo;
    private final ScheduledTaskRepo<ProcessScoresAndExtensionsTaskDef> processScoresTaskRepo;
    private final ScheduledTaskRepo<ZeroOutSubmissionsTaskDef> zeroOutSubmissionsTaskRepo;
    private final ScheduledTaskRepo<PostToCanvasTaskDef> postToCanvasTaskRepo;
    private final ExtensionService extensionService;
    private final CourseService courseService;
    private final AssignmentService assignmentService;
    private final ApplicationEventPublisher eventPublisher;
    private final RabbitMqService rabbitMqService;
    private final PolicyServerService policyServerService;
    private final RawScoreRepo rawScoreRepo;
    private final MasterMigrationStatsRepo masterMigrationStatsRepo;
    private final PolicyService policyService;
    private final CourseMemberService courseMemberService;
    private final ImpersonationManager impersonationManager;
    private final CanvasService canvasService;


    public MigrationService(MigrationRepo migrationRepo, MasterMigrationRepo masterMigrationRepo, MigrationTransactionLogRepo transactionLogRepo, ScheduledTaskRepo<ProcessScoresAndExtensionsTaskDef> taskRepo, ScheduledTaskRepo<ZeroOutSubmissionsTaskDef> zeroOutSubmissionsTaskRepo, ScheduledTaskRepo<PostToCanvasTaskDef> postToCanvasTaskRepo, ExtensionService extensionService, CourseService courseService, AssignmentService assignmentService, ApplicationEventPublisher eventPublisher, RabbitMqService rabbitMqService, PolicyServerService policyServerService, RawScoreRepo rawScoreRepo, MasterMigrationStatsRepo masterMigrationStatsRepo, PolicyService policyService, CourseMemberService courseMemberService, ImpersonationManager impersonationManager, CanvasService canvasService){
        this.migrationRepo = migrationRepo;
        this.masterMigrationRepo = masterMigrationRepo;
        this.transactionLogRepo = transactionLogRepo;
        this.processScoresTaskRepo = taskRepo;
        this.zeroOutSubmissionsTaskRepo = zeroOutSubmissionsTaskRepo;
        this.postToCanvasTaskRepo = postToCanvasTaskRepo;
        this.extensionService = extensionService;
        this.courseService = courseService;
        this.assignmentService = assignmentService;
        this.eventPublisher = eventPublisher;
        this.rabbitMqService = rabbitMqService;
        this.policyServerService = policyServerService;
        this.rawScoreRepo = rawScoreRepo;
        this.masterMigrationStatsRepo = masterMigrationStatsRepo;
        this.policyService = policyService;
        this.courseMemberService = courseMemberService;
        this.impersonationManager = impersonationManager;
        this.canvasService = canvasService;
    }

    public MasterMigration createMigrationForAssignments(Course course, User createdByUser, List<Policy> policyList, List<Assignment> assignmentList){
        MasterMigration masterMigration = new MasterMigration();
        masterMigration.setCourse(course);
        masterMigration.setCreatedByUser(createdByUser);
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

    public List<MasterMigration> getAllMasterMigrations(String courseId){
       return masterMigrationRepo.getMasterMigrationsByCourseId(UUID.fromString(courseId));
    }

    public MasterMigration getMasterMigration(String masterMigrationId) {
        Optional<MasterMigration> masterMigration = masterMigrationRepo.getMasterMigrationById(UUID.fromString(masterMigrationId));
        if (masterMigration.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("Failed to find master migration '%s'", masterMigrationId));
        }
        return masterMigration.get();
    }

    public MasterMigration createMasterMigration(String courseId, User createdByUser){
        MasterMigration masterMigration = new MasterMigration();
        Course course = courseService.getCourse(UUID.fromString(courseId));

        masterMigration.setCourse(course);
        masterMigration.setCreatedByUser(createdByUser);
        return masterMigrationRepo.save(masterMigration);
    }

    public boolean deleteMasterMigration(UUID courseId, UUID masterMigrationId) {
        Optional<MasterMigration> masterMigration = masterMigrationRepo.getMasterMigrationById(masterMigrationId);

        if (masterMigration.isEmpty()){
            log.warn("Attempt to get master migration that doesn't exist!");
            return false;
        }

        masterMigrationRepo.delete(masterMigration.get());

        return true;
    }

    public MasterMigration addMigration(String masterMigrationId, String assignmentId){
        MasterMigration masterMigration = getMasterMigration(masterMigrationId);

        Migration migration = new Migration();
        Assignment assignment = assignmentService.getAssignmentById(assignmentId);

        List<Migration> migrations = masterMigration.getMigrations();
        migrations.add(migration);
        masterMigration.setMigrations(migrations);

        migration.setAssignment(assignment);
        migration.setMasterMigration(masterMigration);
        migrationRepo.save(migration);
        masterMigrationRepo.save(masterMigration);

        masterMigration = getMasterMigration(masterMigrationId);

        return masterMigration;
    }

    public List<Migration> getMigrationsByMasterMigration(String masterMigrationId){
        return migrationRepo.getMigrationListByMasterMigrationId(UUID.fromString(masterMigrationId));
    }

    public Migration getMigration(String migrationId){
        return migrationRepo.getMigrationById(UUID.fromString(migrationId));
    }

    public Course getCourseForMigration(String migrationId){
        return courseService.getCourseForMigration(migrationId);
    }

    public Course getCourseForMasterMigration(String migrationId){
        return courseService.getCourseForMasterMigration(migrationId);
    }

    public Assignment getAssignmentForMigration(String migrationId){
        return assignmentService.getAssignmentForMigration(migrationId);
    }

    public Migration setPolicyForMigration(String migrationId, String policyId){
        Migration updatedMigration = migrationRepo.getMigrationById(UUID.fromString(migrationId));

        Policy policy = policyService.getPolicy(UUID.fromString(policyId));

        policyService.incrementUsedBy(policy);

        if (updatedMigration.getPolicy() != null){
            policyService.decrementUsedBy(updatedMigration.getPolicy());
            updatedMigration.setPolicy(null);
        }

        updatedMigration.setPolicy(policy);
        return migrationRepo.save(updatedMigration);

    }

    public void handleScoreReceived(User asUser, UUID migrationId, ScoredDTO dto){
        MigrationTransactionLog entry = new MigrationTransactionLog();
        List<MigrationTransactionLog> existing = transactionLogRepo.getByCwidAndMigrationId(dto.getCwid(), migrationId);

        if (!existing.isEmpty()){
            log.info("Overwriting exising score for {} under rev {}", dto.getCwid(), existing.getLast().getRevision()+1);
            entry.setRevision(existing.getLast().getRevision()+1);
        }

        entry.setPerformedByUser(asUser);
        entry.setCwid(dto.getCwid());
        entry.setCanvasId(courseMemberService.getCanvasIdGivenCourseAndCwid(dto.getCwid(), getCourseForMigration(migrationId.toString())));
        entry.setMigrationId(migrationId);
        entry.setExtensionId(dto.getExtensionId());
        if (entry.getExtensionId() != null){
            entry.setExtensionApplied(dto.getExtensionStatus().equals(LateRequestStatus.APPLIED));
        }
        else {
            entry.setExtensionApplied(false);
        }
        entry.setSubmissionStatus(dto.getSubmissionStatus());
        entry.setScore(dto.getFinalScore());
        entry.setSubmissionTime(dto.getAdjustedSubmissionTime());

        StringBuilder msg = new StringBuilder();
        Optional<LateRequest> lateRequest = extensionService.getLateRequest(entry.getExtensionId());

        if (entry.isExtensionApplied() && lateRequest.isPresent()){
            msg.append(String.format("Applied extension '%s' submitted on '%s' for %s days\n\n", lateRequest.get().getLateRequestType(), lateRequest.get().getSubmissionDate().toString(), lateRequest.get().getDaysRequested()));
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

    public void processScoresAndExtensionsTask(ProcessScoresAndExtensionsTaskDef task){
        Assignment assignment = assignmentService.getAssignmentById(task.getAssignmentId().toString());

        MigrationFactory.ProcessScoresAndExtensionsConfig config;

        try {
           config = MigrationFactory.startProcessScoresAndExtensions(task.getMigrationId(), rabbitMqService::createRawGradePublishChannel, rabbitMqService::createScoreReceivedChannel)
                    .forAssignment(assignment)
                    .withPolicy(task.getPolicy())
                    .withOnScoreReceived(dto -> this.handleScoreReceived(task.getCreatedByUser(), task.getMigrationId(), dto))
                    .build();
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }

        boolean status = policyServerService.startGrading(config.getGradingStartDTO());

        if (!status){
            throw new RuntimeException("Failed to start grading!");
        }

        List<RawScore> scores = rawScoreRepo.getByMigrationId(task.getMigrationId());

        Map<String, LateRequest> lateRequests = extensionService.getLateRequestsForAssignment(task.getAssignmentId());

        for (RawScore score : scores){
            Optional<LateRequest> extension = Optional.ofNullable(lateRequests.getOrDefault(score.getCwid(), null));
            RawGradeDTO dto = createRawGradeDTO(score, extension);

            rabbitMqService.sendScore(config.getRawGradePublishChannel(), config.getGradingStartDTO().getRawGradeRoutingKey(), dto);
        }
    }

    public void zeroOutSubmissions(ZeroOutSubmissionsTaskDef task){
        Course course = getCourseForMigration(task.getMigrationId().toString());

        List<CourseMember> members = courseMemberService.getAllStudentsInCourse(course);

        for (CourseMember member : members){
            ScoredDTO scoredDTO = new ScoredDTO();
            scoredDTO.setCwid(member.getUser().getCwid());
            scoredDTO.setRawScore(0);
            scoredDTO.setFinalScore(0);
            scoredDTO.setAdjustedSubmissionTime(Instant.now());
            scoredDTO.setHoursLate(0);
            scoredDTO.setSubmissionStatus(SubmissionStatus.MISSING);
            scoredDTO.setExtensionStatus(LateRequestStatus.NO_EXTENSION);

            handleScoreReceived(task.getCreatedByUser(), task.getMigrationId(), scoredDTO);
        }

    }

    private static @NotNull RawGradeDTO createRawGradeDTO(RawScore score, Optional<LateRequest> extension) {
        RawGradeDTO dto = new RawGradeDTO();
        dto.setCwid(score.getCwid());
        dto.setRawScore(score.getScore() == null ? 0 : score.getScore());
        dto.setSubmissionDate(score.getSubmissionTime());
        dto.setSubmissionStatus(score.getSubmissionStatus());

        if (extension.isPresent()){
            dto.setExtensionId(extension.get().getId().toString());
            dto.setExtensionDate(extension.get().getExtensionDate());
            dto.setExtensionDays(extension.get().getDaysRequested());
            dto.setExtensionType(extension.get().getLateRequestType().name());
            dto.setExtensionStatus(extension.get().getStatus());
        }
        return dto;
    }

    public List<ScheduledTaskDef> startProcessScoresAndExtensions(User actingUser, String masterMigrationId ){
        if(!validateApplyMasterMigration(masterMigrationId)) {
            return List.of();
        }

        Optional<MasterMigration> master = masterMigrationRepo.getMasterMigrationById(UUID.fromString(masterMigrationId));

        if(master.isEmpty()){
            return List.of();
        }

        if (master.get().getStatus() != MigrationStatus.LOADED){
            log.warn("Migration is in invalid state to start a migration. {} != {}", master.get().getStatus().name(), MigrationStatus.LOADED.name());
            return List.of();
        }

        List<Migration> migrations = migrationRepo.getMigrationListByMasterMigrationId(UUID.fromString(masterMigrationId));

        log.info("Starting migration for {} assignments", migrations.size());

        List<ScheduledTaskDef> tasks = new LinkedList<>();

        for (Migration migration : migrations){
            Assignment assignment = migration.getAssignment();

            ZeroOutSubmissionsTaskDef zeroOutSubmissionsTask = new ZeroOutSubmissionsTaskDef();
            zeroOutSubmissionsTask.setCreatedByUser(actingUser);
            zeroOutSubmissionsTask.setTaskName(String.format("Initializing all scores to zero / missing for assignment '%s'", assignment.getName()));
            zeroOutSubmissionsTask.setMigrationId(migration.getId());
            zeroOutSubmissionsTask = zeroOutSubmissionsTaskRepo.save(zeroOutSubmissionsTask);

            tasks.add(zeroOutSubmissionsTask);

            NewTaskEvent.TaskData<ZeroOutSubmissionsTaskDef> zeroTaskDef = new NewTaskEvent.TaskData<>(zeroOutSubmissionsTaskRepo, zeroOutSubmissionsTask.getId(), this::zeroOutSubmissions);

            ProcessScoresAndExtensionsTaskDef task = new ProcessScoresAndExtensionsTaskDef();
            task.setCreatedByUser(actingUser);
            task.setTaskName(String.format("Process scores and extensions for assignment '%s'", assignment.getName()));
            task.setMigrationId(migration.getId());
            task.setAssignmentId(assignment.getId());
            task.setPolicy(URI.create(migration.getPolicy().getPolicyURI()));
            task = processScoresTaskRepo.save(task);

            tasks.add(task);

            NewTaskEvent.TaskData<ProcessScoresAndExtensionsTaskDef> taskDefinition = new NewTaskEvent.TaskData<>(processScoresTaskRepo, task.getId(), this::processScoresAndExtensionsTask);
            taskDefinition.setDependsOn(Set.of(zeroOutSubmissionsTask.getId()));

            eventPublisher.publishEvent(new NewTaskEvent(this, zeroTaskDef));
            eventPublisher.publishEvent(new NewTaskEvent(this, taskDefinition));
        }

        master.get().setStatus(MigrationStatus.STARTED);

        masterMigrationRepo.save(master.get());

        return tasks;
    }

    public Optional<MasterMigrationStats> getStatsForMigration(String masterMigrationId) {
        return masterMigrationStatsRepo.findById(UUID.fromString(masterMigrationId));
    }

    public boolean attemptToStartRawScoreImport(String migrationId, String message, ExternalAssignmentType type){
        Migration migration = getMigration(migrationId);

        if (migration.getRawScoreStatus() != RawScoreStatus.EMPTY){
            return false;
        }

        migration.setRawScoreStatus(RawScoreStatus.IMPORTING);
        migration.setRawScoreMessage(message);
        migration.setRawScoreType(type);

        migrationRepo.save(migration);

        return true;
    }

    public boolean finishRawScoreImport(String migrationId, String message){
        Migration migration = getMigration(migrationId);

        if (migration.getRawScoreStatus() != RawScoreStatus.IMPORTING){
            return false;
        }

        migration.setRawScoreStatus(RawScoreStatus.PRESENT);
        migration.setRawScoreMessage(message);

        migrationRepo.save(migration);

        return true;
    }


    public boolean validateLoadMasterMigration(String masterMigrationId){
        MasterMigration masterMigration = getMasterMigration(masterMigrationId);


        if (masterMigration.getStatus() != MigrationStatus.CREATED){
            log.error("Attempt to load migration NOT in created state! Actual state: '{}'", masterMigration.getStatus());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("Attempt to load migration NOT in created state! Actual state: '%s'", masterMigration.getStatus()));
        }

        List<Migration> migrations = getMigrationsByMasterMigration(masterMigrationId);

        List<String> errors = new LinkedList<>();

        for (Migration m : migrations){
            if (m.getRawScoreStatus() != RawScoreStatus.PRESENT){
                Assignment assignmentName = getAssignmentForMigration(m.getId().toString());
                log.error("Migration for assignment '{}' is missing raw scores!", assignmentName.getName());
                errors.add(String.format("Migration for assignment '%s' is missing raw scores!", assignmentName.getName()));
            }
        }

        if (!errors.isEmpty()){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    String.join("\n", errors)
            );
        }

        return true;
    }

    public boolean finalizeLoadMasterMigration(String masterMigrationId){
        if (!validateLoadMasterMigration(masterMigrationId)){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to validate load phase");
        }

        MasterMigration masterMigration = getMasterMigration(masterMigrationId);

        masterMigration.setStatus(MigrationStatus.LOADED);

        masterMigrationRepo.save(masterMigration);
        return true;
    }

    public boolean validateApplyMasterMigration(String masterMigrationId){
        MasterMigration masterMigration = getMasterMigration(masterMigrationId);

        List<String> errors = new LinkedList<>();

        for (Migration migration : masterMigration.getMigrations()){
            if (migration.getPolicy() != null && migration.getRawScoreStatus() == RawScoreStatus.PRESENT){
                continue;
            }

            log.error("Either the policy or the raw scores are not set");
            errors.add("Either the policy or the raw scores are not set");
        }

        if (!errors.isEmpty()){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    String.join("\n", errors)
            );
        }

        return true;
    }

    public List<MigrationWithScoresDTO> getMasterMigrationToReview(String masterMigrationId){
        MasterMigration masterMigration = getMasterMigration(masterMigrationId);

        masterMigration.setStatus(MigrationStatus.AWAITING_REVIEW);

        masterMigration = masterMigrationRepo.save(masterMigration);


        List<MigrationWithScoresDTO> migrationWithScoresDTOs = new LinkedList<>();
        Course course = getCourseForMasterMigration(masterMigrationId);

        for(Migration migration : masterMigration.getMigrations()){
            AssignmentSlimDTO assignmentSlimDTO = DTOFactory.toSlimDto(migration.getAssignment());

            MigrationWithScoresDTO migrationWithScoresDTO = new MigrationWithScoresDTO();
            List<MigrationTransactionLog> entries = transactionLogRepo.getAllByMigrationIdSorted(migration.getId());
            Map<String, ScoreDTO> scores = new HashMap<>();

            for (MigrationTransactionLog entry : entries){
                Optional<CourseMember> member = courseMemberService.findCourseMemberGivenCourseAndCwid(course, entry.getCwid());

                if (member.isEmpty()){
                    log.warn("Skipping student '{}' without enrollment in course", entry.getCwid());
                    continue;
                }

                ScoreDTO score = new ScoreDTO();
                score.setScore(entry.getScore());
                score.setStatus(entry.getSubmissionStatus().getStatus());
                score.submissionDate(entry.getSubmissionTime());
                score.setComment(entry.getMessage());
                score.setRawScore(rawScoreRepo.getByCwidAndMigrationId(entry.getCwid(), entry.getMigrationId()).map(RawScore::getScore).orElse(0.));
                score.daysLate(rawScoreRepo.getByCwidAndMigrationId(entry.getCwid(), entry.getMigrationId()).map(r -> (int)(r.getHoursLate() == null ? 0 : r.getHoursLate() / 24)).orElse(0));


                score.setStudent(DTOFactory.toDto(member.get()));

                scores.put(entry.getCwid(), score);
            }

            if (scores.isEmpty()){
                log.error("No students were scored!");
                log.warn("Assigment '{}' will be excluded!", assignmentSlimDTO.getName());
                continue;
            }

            log.info("'{}' students were scored", scores.size());

            migrationWithScoresDTO.setMigrationId(migration.getId().toString());
            migrationWithScoresDTO.setAssignment(assignmentSlimDTO);
            migrationWithScoresDTO.setScores(scores.values().stream().toList());

            migrationWithScoresDTOs.add(migrationWithScoresDTO);
        }

        return migrationWithScoresDTOs;
    }

    public boolean updateStudentScore(User asUser, String migrationId, MigrationScoreChangeDTO dto){
        ScoredDTO scored = new ScoredDTO();
        scored.setCwid(dto.getCwid());
        scored.setExtensionStatus(LateRequestStatus.NO_EXTENSION);
        scored.setFinalScore(dto.getNewScore());
        scored.setRawScore(dto.getNewScore());
        scored.setAdjustedSubmissionTime(dto.getAdjustedSubmissionDate());
        scored.setSubmissionStatus(SubmissionStatus.fromString(dto.getSubmissionStatus().getValue()));
        scored.setSubmissionMessage(dto.getJustification());

        handleScoreReceived(asUser, UUID.fromString(migrationId), scored);

        return true;
    }

    public boolean finalizeReviewMasterMigration(String masterMigrationId){
        MasterMigration masterMigration = getMasterMigration(masterMigrationId);

        if (!masterMigration.getStatus().equals(MigrationStatus.AWAITING_REVIEW)){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Migration not in Awaiting Review state!");
        }

        masterMigration.setStatus(MigrationStatus.READY_TO_POST);

        masterMigrationRepo.save(masterMigration);

        return true;
    }

    public void postGradesToCanvasTask(PostToCanvasTaskDef taskDef){
        IdentityProvider provider = impersonationManager.impersonateUser(taskDef.getCreatedByUser());

        CanvasService.BuiltAssignmentSubmissions submissions = canvasService.prepCanvasSubmissionsForPublish(String.valueOf(taskDef.getCanvasCourseId()), taskDef.getCanvasAssignmentId());

        List<MigrationTransactionLog> entries = transactionLogRepo.getAllByMigrationIdSorted(taskDef.getMigrationId());

        log.info("Processing {} migration log entries for posting to canvas for migration '{}'", entries.size(), taskDef.getMigrationId());

        for (MigrationTransactionLog entry : entries){
            submissions.addSubmission(entry.getCanvasId(), entry.getMessage(), entry.getScore(), entry.getSubmissionStatus().equals(SubmissionStatus.EXCUSED));
        }

        Optional<Progress> progress = canvasService.asUser(provider).publishCanvasScores(submissions);

        if (progress.isEmpty()){
            throw new RuntimeException("Failed to post scores to Canvas!");
        }

        // We will probably want to periodically check in on this and then only flag this as completed once this is done
    }

    public List<ScheduledTaskDef> processMigrationLog(User actingUser, String masterMigrationId){
        MasterMigration masterMigration = getMasterMigration(masterMigrationId);

        if (masterMigration.getStatus() != MigrationStatus.READY_TO_POST){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("Master migration is in invalid state to post! Expected: %s Was: %s", MigrationStatus.READY_TO_POST, masterMigration.getStatus()));
        }

        masterMigration.setStatus(MigrationStatus.POSTING);
        masterMigrationRepo.save(masterMigration);

        Course course = getCourseForMasterMigration(masterMigrationId);

        List<ScheduledTaskDef> tasks = new LinkedList<>();

        for (Migration migration : masterMigration.getMigrations()) {
            Assignment assignment = getAssignmentForMigration(migration.getId().toString());

            PostToCanvasTaskDef task = new PostToCanvasTaskDef();
            task.setCreatedByUser(actingUser);
            task.setTaskName(String.format("Post scores for '%s' to Canvas", assignment.getName()));
            task.setMigrationId(migration.getId());
            task.setCanvasAssignmentId(assignment.getCanvasId());
            task.setCanvasCourseId(course.getCanvasId());
            task = postToCanvasTaskRepo.save(task);

            tasks.add(task);

            NewTaskEvent.TaskData<PostToCanvasTaskDef> taskDefinition = new NewTaskEvent.TaskData<>(postToCanvasTaskRepo, task.getId(), this::postGradesToCanvasTask);

            eventPublisher.publishEvent(new NewTaskEvent(this, taskDefinition));
        }

        return tasks;
    }

    public boolean finalizePostToCanvas(String masterMigrationId){
        MasterMigration masterMigration = getMasterMigration(masterMigrationId);


        if (!masterMigration.getStatus().equals(MigrationStatus.POSTING)){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("Invalid status for finalizing migration! Expected: %s Was: %s", MigrationStatus.POSTING, masterMigration.getStatus()));
        }

        masterMigration.setStatus(MigrationStatus.COMPLETED);
        masterMigrationRepo.save(masterMigration);
        return true;
    }



}
