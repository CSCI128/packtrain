package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.data.AssignmentSlimDTO;
import edu.mines.gradingadmin.data.MigrationScoreChangeDTO;
import edu.mines.gradingadmin.data.MigrationWithScoresDTO;
import edu.mines.gradingadmin.data.ScoreDTO;
import edu.mines.gradingadmin.data.policyServer.RawGradeDTO;
import edu.mines.gradingadmin.data.policyServer.ScoredDTO;
import edu.mines.gradingadmin.events.NewTaskEvent;
import edu.mines.gradingadmin.factories.DTOFactory;
import edu.mines.gradingadmin.factories.MigrationFactory;
import edu.mines.gradingadmin.models.*;
import edu.mines.gradingadmin.models.enums.*;
import edu.mines.gradingadmin.models.tasks.ProcessScoresAndExtensionsTaskDef;
import edu.mines.gradingadmin.models.tasks.ScheduledTaskDef;
import edu.mines.gradingadmin.models.tasks.ZeroOutSubmissionsTaskDef;
import edu.mines.gradingadmin.repositories.*;
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


    public MigrationService(MigrationRepo migrationRepo, MasterMigrationRepo masterMigrationRepo, MigrationTransactionLogRepo transactionLogRepo, ScheduledTaskRepo<ProcessScoresAndExtensionsTaskDef> taskRepo, ScheduledTaskRepo<ZeroOutSubmissionsTaskDef> zeroOutSubmissionsTaskRepo, ExtensionService extensionService, CourseService courseService, AssignmentService assignmentService, ApplicationEventPublisher eventPublisher, RabbitMqService rabbitMqService, PolicyServerService policyServerService, RawScoreRepo rawScoreRepo, MasterMigrationStatsRepo masterMigrationStatsRepo, PolicyService policyService, CourseMemberService courseMemberService){
        this.migrationRepo = migrationRepo;
        this.masterMigrationRepo = masterMigrationRepo;
        this.transactionLogRepo = transactionLogRepo;
        this.processScoresTaskRepo = taskRepo;
        this.zeroOutSubmissionsTaskRepo = zeroOutSubmissionsTaskRepo;
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

    @Transactional
    public List<MasterMigration> getAllMasterMigrations(String courseId){
       return masterMigrationRepo.getMasterMigrationsByCourseId(UUID.fromString(courseId));
    }


    public Optional<MasterMigration> createMasterMigration(String courseId, User createdByUser){
        MasterMigration masterMigration = new MasterMigration();
        Optional<Course> course = courseService.getCourse(UUID.fromString(courseId));
        if (course.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course does not exist");
        }
        masterMigration.setCourse(course.get());
        masterMigration.setCreatedByUser(createdByUser);
        return Optional.of(masterMigrationRepo.save(masterMigration));
    }

    public Optional<MasterMigration> addMigration(String masterMigrationId, String assignmentId){
        Optional<MasterMigration> masterMigration = masterMigrationRepo.getMasterMigrationById(UUID.fromString(masterMigrationId));

        if (masterMigration.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Master migration does not exist");
        }

        Migration migration = new Migration();
        Optional<Assignment> assignment = assignmentService.getAssignmentById(assignmentId);

        if (assignment.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment does not exist");
        }

        migration.setAssignment(assignment.get());
        migration.setMasterMigration(masterMigration.get());
        migrationRepo.save(migration);

        return masterMigrationRepo.getMasterMigrationById(UUID.fromString(masterMigrationId));
    }

    public List<Migration> getMigrationsByMasterMigration(String masterMigrationId){
        return migrationRepo.getMigrationListByMasterMigrationId(UUID.fromString(masterMigrationId));
    }

    public Migration getMigration(String migrationId){
        return migrationRepo.getMigrationById(UUID.fromString(migrationId));
    }

    @Transactional
    public Course getCourseForMigration(String migrationId){
        Migration migration = getMigration(migrationId);

        MasterMigration master = migration.getMasterMigration();

        return master.getCourse();
    }

    @Transactional
    public Course getCourseForMasterMigration(String migrationId){

        MasterMigration master = getMasterMigration(migrationId).orElseThrow();

        return master.getCourse();
    }

    @Transactional
    public Assignment getAssignmentForMigration(String migrationId){
        Migration migration = getMigration(migrationId);

        return migration.getAssignment();
    }

    public Optional<Migration> setPolicyForMigration(String migrationId, String policyId){
        Migration updatedMigration = migrationRepo.getMigrationById(UUID.fromString(migrationId));

        Optional<Policy> policy = policyService.getPolicy(UUID.fromString(policyId)).map(policyService::incrementUsedBy);

        if (updatedMigration.getPolicy() != null){
            policyService.decrementUsedBy(updatedMigration.getPolicy());
            updatedMigration.setPolicy(null);
        }

        if (policy.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Policy does not exist");
        }

        updatedMigration.setPolicy(policy.get());
        return Optional.of(migrationRepo.save(updatedMigration));

    }

    public void handleScoreReceived(User asUser, UUID migrationId, ScoredDTO dto){
        MigrationTransactionLog entry = new MigrationTransactionLog();
        Optional<MigrationTransactionLog> existing = transactionLogRepo.getByCwidAndMigrationId(dto.getCwid(), migrationId);

        if (existing.isPresent()){
            log.info("Overwriting exising score for {} under rev {}", dto.getCwid(), existing.get().getRevision()+1);
            entry.setRevision(existing.get().getRevision()+1);
        }

        entry.setPerformedByUser(asUser);
        entry.setCwid(dto.getCwid());
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
        Optional<Assignment> assignment = assignmentService.getAssignmentById(task.getAssignmentId().toString());

        if (assignment.isEmpty()){
            throw new RuntimeException(String.format("Failed to get assignment '%s'", task.getAssignmentId()));
        }
        MigrationFactory.ProcessScoresAndExtensionsConfig config;

        try {
           config = MigrationFactory.startProcessScoresAndExtensions(task.getMigrationId(), rabbitMqService::createRawGradePublishChannel, rabbitMqService::createScoreReceivedChannel)
                    .forAssignment(assignment.get())
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

    @Transactional
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
        dto.setRawScore(score.getScore());
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

    @Transactional
    public List<ScheduledTaskDef> startProcessScoresAndExtensions(User actingUser, String masterMigrationId ){
        Optional<MasterMigration> master = masterMigrationRepo.getMasterMigrationById(UUID.fromString(masterMigrationId));

        if(master.isEmpty()){
            return List.of();
        }

        if (master.get().getStatus() != MigrationStatus.CREATED){
            log.warn("Migration is in invalid state to start a migration. {} != {}", master.get().getStatus().name(), MigrationStatus.CREATED.name());
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
            task.setAssignmentId(migration.getId());
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

    public Optional<MasterMigration> getMasterMigration(String masterMigrationId) {
        return masterMigrationRepo.getMasterMigrationById(UUID.fromString(masterMigrationId));
    }

    public boolean validateLoadMasterMigration(String masterMigrationId){
        Optional<MasterMigration> masterMigration = getMasterMigration(masterMigrationId);

        if (masterMigration.isEmpty()){
            return false;
        }

        if (masterMigration.get().getStatus() != MigrationStatus.CREATED){
            log.error("Attempt to load migration NOT in created state! Actual state: '{}'", masterMigration.get().getStatus());
            return false;
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

        return errors.isEmpty();
    }

    public Optional<MasterMigration> finalizeLoadMasterMigration(String masterMigrationId){
        if (!validateLoadMasterMigration(masterMigrationId)){
            return Optional.empty();
        }

        Optional<MasterMigration> masterMigration = getMasterMigration(masterMigrationId);

        if(masterMigration.isEmpty()){
            return Optional.empty();
        }

        masterMigration.get().setStatus(MigrationStatus.LOADED);


        return Optional.of(masterMigrationRepo.save(masterMigration.get()));
    }

    public boolean validateApplyMasterMigration(String masterMigrationId){
        Optional<MasterMigration> masterMigration = getMasterMigration(masterMigrationId);

        if (masterMigration.isEmpty()){
            return false;
        }

        List<String> errors = new LinkedList<>();

        for (Migration migration : masterMigration.get().getMigrations()){
            if (migration.getPolicy() != null && migration.getRawScoreStatus() == RawScoreStatus.PRESENT){
                continue;
            }

            log.error("Either the policy or the raw scores are not set");
            errors.add("Either the policy or the raw scores are not set");
        }

        return errors.isEmpty();
    }

    @Transactional
    public List<MigrationWithScoresDTO> getMasterMigrationToReview(String masterMigrationId){
        Optional<MasterMigration> masterMigration = getMasterMigration(masterMigrationId);

        if (masterMigration.isEmpty()){
            return List.of();
        }

        masterMigration.get().setStatus(MigrationStatus.AWAITING_REVIEW);

        masterMigrationRepo.save(masterMigration.get());


        List<MigrationWithScoresDTO> migrationWithScoresDTOs = new LinkedList<>();
        Course course = getCourseForMasterMigration(masterMigrationId);

        for(Migration migration : masterMigration.get().getMigrations()){
            AssignmentSlimDTO assignmentSlimDTO = DTOFactory.toSlimDto(migration.getAssignment());

            MigrationWithScoresDTO migrationWithScoresDTO = new MigrationWithScoresDTO();
            List<MigrationTransactionLog> entries = transactionLogRepo.getAllByMigrationIdSorted(migration.getId());
            Map<String, ScoreDTO> scores = new HashMap<>();

            for (MigrationTransactionLog entry : entries){
                Optional<CourseMember> member = courseMemberService.getCourseMemberByCourseByCwid(course, entry.getCwid());

                if (member.isEmpty()){
                    log.warn("Skipping student '{}' without enrollment in course", entry.getCwid());
                    continue;
                }

                ScoreDTO score = new ScoreDTO();
                score.setScore(entry.getScore());
                score.setStatus(entry.getSubmissionStatus().getStatus());
                score.submissionDate(entry.getSubmissionTime());
                score.setComment(entry.getMessage());


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
        Optional<MasterMigration> masterMigration = getMasterMigration(masterMigrationId);

        if (masterMigration.isEmpty()){
            return false;
        }

        masterMigration.get().setStatus(MigrationStatus.READY_TO_POST);

        return true;
    }

}
