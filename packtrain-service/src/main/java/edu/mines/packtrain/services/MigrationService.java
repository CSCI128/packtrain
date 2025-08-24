package edu.mines.packtrain.services;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import edu.ksu.canvas.model.Progress;
import edu.mines.packtrain.data.AssignmentSlimDTO;
import edu.mines.packtrain.data.MigrationScoreChangeDTO;
import edu.mines.packtrain.data.MigrationWithScoresDTO;
import edu.mines.packtrain.data.PolicyRawScoreDTO;
import edu.mines.packtrain.data.ScoreDTO;
import edu.mines.packtrain.data.PolicyRawScoreDTO.ExtensionStatusEnum;
import edu.mines.packtrain.data.PolicyRawScoreDTO.SubmissionStatusEnum;
import edu.mines.packtrain.data.policyServer.ScoredDTO;
import edu.mines.packtrain.events.NewTaskEvent;
import edu.mines.packtrain.factories.DTOFactory;
import edu.mines.packtrain.factories.MigrationFactory;
import edu.mines.packtrain.managers.IdentityProvider;
import edu.mines.packtrain.managers.ImpersonationManager;
import edu.mines.packtrain.models.Assignment;
import edu.mines.packtrain.models.Course;
import edu.mines.packtrain.models.CourseMember;
import edu.mines.packtrain.models.LateRequest;
import edu.mines.packtrain.models.MasterMigration;
import edu.mines.packtrain.models.MasterMigrationStats;
import edu.mines.packtrain.models.Migration;
import edu.mines.packtrain.models.MigrationTransactionLog;
import edu.mines.packtrain.models.Policy;
import edu.mines.packtrain.models.RawScore;
import edu.mines.packtrain.models.User;
import edu.mines.packtrain.models.enums.ExternalAssignmentType;
import edu.mines.packtrain.models.enums.LateRequestStatus;
import edu.mines.packtrain.models.enums.MigrationStatus;
import edu.mines.packtrain.models.enums.RawScoreStatus;
import edu.mines.packtrain.models.enums.SubmissionStatus;
import edu.mines.packtrain.models.tasks.PostToCanvasTaskDef;
import edu.mines.packtrain.models.tasks.ProcessScoresAndExtensionsTaskDef;
import edu.mines.packtrain.models.tasks.ScheduledTaskDef;
import edu.mines.packtrain.models.tasks.ZeroOutSubmissionsTaskDef;
import edu.mines.packtrain.repositories.MasterMigrationRepo;
import edu.mines.packtrain.repositories.MasterMigrationStatsRepo;
import edu.mines.packtrain.repositories.MigrationRepo;
import edu.mines.packtrain.repositories.MigrationTransactionLogRepo;
import edu.mines.packtrain.repositories.RawScoreRepo;
import edu.mines.packtrain.repositories.ScheduledTaskRepo;
import edu.mines.packtrain.services.external.CanvasService;
import edu.mines.packtrain.services.external.PolicyServerService;
import edu.mines.packtrain.services.external.RabbitMqService;
import lombok.extern.slf4j.Slf4j;

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

    public List<MasterMigration> getAllMasterMigrations(UUID courseId){
       return masterMigrationRepo.getMasterMigrationsByCourseId(courseId);
    }

    public MasterMigration getMasterMigration(UUID masterMigrationId) {
        Optional<MasterMigration> masterMigration = masterMigrationRepo.getMasterMigrationById(masterMigrationId);
        if (masterMigration.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("Failed to find master migration '%s'", masterMigrationId));
        }
        return masterMigration.get();
    }

    public MasterMigration createMasterMigration(UUID courseId, User createdByUser){
        MasterMigration masterMigration = new MasterMigration();
        Course course = courseService.getCourse(courseId);

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

    public MasterMigration addMigration(UUID masterMigrationId, UUID assignmentId){
        MasterMigration masterMigration = getMasterMigration(masterMigrationId);

        Migration migration = new Migration();
        Assignment assignment = assignmentService.getAssignmentById(assignmentId);

        List<Migration> migrations = masterMigration.getMigrations();
        if(migrations == null) {
            migrations = new ArrayList<>();
        }
        migrations.add(migration);
        masterMigration.setMigrations(migrations);

        migration.setAssignment(assignment);
        migration.setMasterMigration(masterMigration);
        migrationRepo.save(migration);
        masterMigrationRepo.save(masterMigration);

        masterMigration = getMasterMigration(masterMigrationId);

        return masterMigration;
    }

    public List<Migration> getMigrationsByMasterMigration(UUID masterMigrationId){
        return migrationRepo.getMigrationListByMasterMigrationId(masterMigrationId);
    }

    public Migration getMigration(UUID migrationId){
        return migrationRepo.getMigrationById(migrationId);
    }

    public Course getCourseForMigration(UUID migrationId){
        return courseService.getCourseForMigration(migrationId);
    }

    public Course getCourseForMasterMigration(UUID migrationId){
        return courseService.getCourseForMasterMigration(migrationId);
    }

    public Assignment getAssignmentForMigration(UUID migrationId){
        // will clean this up - out of scope for this ticket
        Migration migration = getMigration(migrationId);
        return assignmentService.getAssignmentForMigration(migration);
    }

    public Migration setPolicyForMigration(UUID migrationId, UUID policyId){
        Migration updatedMigration = migrationRepo.getMigrationById(migrationId);

        Policy policy = policyService.getPolicy(policyId);

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
        entry.setCanvasId(courseMemberService.getCanvasIdGivenCourseAndCwid(dto.getCwid(), getCourseForMigration(migrationId)));
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
        Assignment assignment = assignmentService.getAssignmentById(task.getAssignmentId());

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
            PolicyRawScoreDTO dto = createRawGradeDTO(score, extension);

            if(!courseMemberService.isUserEnrolledInCourse(score.getCwid(), assignment.getCourse())){
                log.warn("User '{}' is not enrolled in course '{}'", score, assignment.getCourse());
                continue;
            }

            rabbitMqService.sendScore(config.getRawGradePublishChannel(), config.getGradingStartDTO().getRawGradeRoutingKey(), dto);
        }
    }

    public void zeroOutSubmissions(ZeroOutSubmissionsTaskDef task){
        Course course = getCourseForMigration(task.getMigrationId());

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

    private static @NotNull PolicyRawScoreDTO createRawGradeDTO(RawScore score, Optional<LateRequest> extension) {
        PolicyRawScoreDTO dto = new PolicyRawScoreDTO();
        dto.setCwid(score.getCwid());
        dto.setRawScore(score.getScore() == null ? 0 : score.getScore());
        dto.setSubmissionDate(score.getSubmissionTime());
        dto.setSubmissionStatus(SubmissionStatusEnum.fromValue(score.getSubmissionStatus().getStatus()));

        if (extension.isPresent()){
            dto.setExtensionId(extension.get().getId());
            dto.setExtensionDate(extension.get().getExtensionDate());
            dto.setExtensionDays(extension.get().getDaysRequested());
            dto.setExtensionType(extension.get().getLateRequestType().name());
            dto.setExtensionStatus(ExtensionStatusEnum.fromValue(extension.get().getStatus().getStatus()));
        }
        return dto;
    }

    public List<ScheduledTaskDef> startProcessScoresAndExtensions(User actingUser, UUID masterMigrationId ){
        if(!validateApplyMasterMigration(masterMigrationId)) {
            return List.of();
        }

        Optional<MasterMigration> master = masterMigrationRepo.getMasterMigrationById(masterMigrationId);

        if(master.isEmpty()){
            return List.of();
        }

        if (master.get().getStatus() != MigrationStatus.LOADED){
            log.warn("Migration is in invalid state to start a migration. {} != {}", master.get().getStatus().name(), MigrationStatus.LOADED.name());
            return List.of();
        }

        List<Migration> migrations = migrationRepo.getMigrationListByMasterMigrationId(masterMigrationId);

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

    public Optional<MasterMigrationStats> getStatsForMasterMigration(UUID masterMigrationId) {
        return masterMigrationStatsRepo.findById(masterMigrationId);
    }

    public boolean attemptToStartRawScoreImport(UUID migrationId, String message, ExternalAssignmentType type){
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

    public boolean finishRawScoreImport(UUID migrationId, String message){
        Migration migration = getMigration(migrationId);

        if (migration.getRawScoreStatus() != RawScoreStatus.IMPORTING){
            return false;
        }

        migration.setRawScoreStatus(RawScoreStatus.PRESENT);
        migration.setRawScoreMessage(message);

        migrationRepo.save(migration);

        return true;
    }


    public boolean validateLoadMasterMigration(UUID masterMigrationId){
        MasterMigration masterMigration = getMasterMigration(masterMigrationId);


        if (masterMigration.getStatus() != MigrationStatus.CREATED){
            log.error("Attempt to load migration NOT in created state! Actual state: '{}'", masterMigration.getStatus());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("Attempt to load migration NOT in created state! Actual state: '%s'", masterMigration.getStatus()));
        }

        List<Migration> migrations = getMigrationsByMasterMigration(masterMigrationId);

        List<String> errors = new LinkedList<>();

        for (Migration m : migrations){
            if (m.getRawScoreStatus() != RawScoreStatus.PRESENT){
                Assignment assignmentName = getAssignmentForMigration(m.getId());
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

    public boolean finalizeLoadMasterMigration(UUID masterMigrationId){
        if (!validateLoadMasterMigration(masterMigrationId)){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to validate load phase");
        }

        MasterMigration masterMigration = getMasterMigration(masterMigrationId);

        masterMigration.setStatus(MigrationStatus.LOADED);

        masterMigrationRepo.save(masterMigration);
        return true;
    }

    public boolean validateApplyMasterMigration(UUID masterMigrationId){
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

    public List<MigrationWithScoresDTO> getMasterMigrationToReview(UUID masterMigrationId){
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
                // TODO this is bad
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

            migrationWithScoresDTO.setMigrationId(migration.getId());
            migrationWithScoresDTO.setAssignment(assignmentSlimDTO);
            migrationWithScoresDTO.setScores(scores.values().stream().toList());

            Optional<MasterMigrationStats> stats = getStatsForMasterMigration(masterMigrationId);

            if (stats.isPresent()){
                migrationWithScoresDTO.setStats(stats.map(DTOFactory::toDto).get());
            }

            migrationWithScoresDTOs.add(migrationWithScoresDTO);

        }

        return migrationWithScoresDTOs;
    }

    public boolean updateStudentScore(User asUser, UUID migrationId, MigrationScoreChangeDTO dto){
        ScoredDTO scored = new ScoredDTO();
        scored.setCwid(dto.getCwid());
        scored.setExtensionStatus(LateRequestStatus.NO_EXTENSION);
        scored.setFinalScore(dto.getNewScore());
        scored.setRawScore(dto.getNewScore());
        scored.setAdjustedSubmissionTime(dto.getAdjustedSubmissionDate());
        scored.setSubmissionStatus(SubmissionStatus.fromString(dto.getSubmissionStatus().getValue()));
        scored.setSubmissionMessage(dto.getJustification());

        handleScoreReceived(asUser, migrationId, scored);

        return true;
    }

    public boolean finalizeReviewMasterMigration(UUID masterMigrationId){
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

    public List<ScheduledTaskDef> processMigrationLog(User actingUser, UUID masterMigrationId){
        MasterMigration masterMigration = getMasterMigration(masterMigrationId);

        if (masterMigration.getStatus() != MigrationStatus.READY_TO_POST){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("Master migration is in invalid state to post! Expected: %s Was: %s", MigrationStatus.READY_TO_POST, masterMigration.getStatus()));
        }

        masterMigration.setStatus(MigrationStatus.POSTING);
        masterMigrationRepo.save(masterMigration);

        Course course = getCourseForMasterMigration(masterMigrationId);

        List<ScheduledTaskDef> tasks = new LinkedList<>();

        for (Migration migration : masterMigration.getMigrations()) {
            Assignment assignment = getAssignmentForMigration(migration.getId());

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

    public boolean finalizePostToCanvas(UUID masterMigrationId){
        MasterMigration masterMigration = getMasterMigration(masterMigrationId);


        if (!masterMigration.getStatus().equals(MigrationStatus.POSTING)){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("Invalid status for finalizing migration! Expected: %s Was: %s", MigrationStatus.POSTING, masterMigration.getStatus()));
        }

        masterMigration.setStatus(MigrationStatus.COMPLETED);
        masterMigrationRepo.save(masterMigration);
        return true;
    }



}
