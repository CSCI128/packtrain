package edu.mines.gradingadmin.services;


import edu.mines.gradingadmin.containers.PostgresTestContainer;
import edu.mines.gradingadmin.data.messages.ScoredDTO;
import edu.mines.gradingadmin.models.*;
import edu.mines.gradingadmin.models.enums.LateRequestStatus;
import edu.mines.gradingadmin.models.enums.SubmissionStatus;
import edu.mines.gradingadmin.models.tasks.ProcessScoresAndExtensionsTaskDef;
import edu.mines.gradingadmin.repositories.*;
import edu.mines.gradingadmin.seeders.CourseSeeders;
import edu.mines.gradingadmin.seeders.UserSeeders;
import edu.mines.gradingadmin.services.external.PolicyServerService;
import edu.mines.gradingadmin.services.external.RabbitMqService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@SpringBootTest
public class TestMigrationService implements PostgresTestContainer {
    MigrationService migrationService;

    @Autowired
    private MigrationRepo migrationRepo;
    @Autowired
    private CourseSeeders courseSeeders;
    @Autowired
    private CourseService courseService;
    @Autowired
    private UserSeeders userSeeders;
    @Autowired
    private MasterMigrationRepo masterMigrationRepo;
    @Autowired
    private PolicyRepo policyRepo;
    @Autowired
    private AssignmentService assignmentService;
    @Autowired
    private MigrationTransactionLogRepo migrationTransactionLogRepo;
    @Autowired
    private ScheduledTaskRepo<ProcessScoresAndExtensionsTaskDef> taskRepo;
    @Autowired
    private MasterMigrationStatsRepo masterMigrationStatsRepo;
    @Autowired
    private ExtensionService extensionService;
    @Autowired
    private RawScoreService rawScoreService;

    @BeforeAll
    static void setupClass(){
        postgres.start();

    }

    @BeforeEach
    void setup(){
        migrationService = new MigrationService(migrationRepo, masterMigrationRepo, migrationTransactionLogRepo, taskRepo,
                extensionService, courseService, assignmentService, Mockito.mock(ApplicationEventPublisher.class),
                Mockito.mock(RabbitMqService.class), Mockito.mock(PolicyServerService.class), rawScoreService, masterMigrationStatsRepo);

    }

    @AfterEach
    void tearDown(){
        migrationRepo.deleteAll();
        masterMigrationRepo.deleteAll();
        policyRepo.deleteAll();
        courseSeeders.clearAll();
        userSeeders.clearAll();
    }

    @Test
    void verifyGetMigrations() {
        Course course1 = courseSeeders.populatedCourse();
        User user = userSeeders.user1();
        String filename = "file.js";
        Policy policy = new Policy();
        policy.setCreatedByUser(user);
        policy.setCourse(course1);
        policy.setPolicyName("test_policy");
        policy.setPolicyURI(filename);
        policyRepo.save(policy);

        MasterMigration masterMigration = migrationService.createMigrationForAssignments(course1, List.of(policy), course1.getAssignments().stream().toList());
        Assertions.assertEquals(1, masterMigrationRepo.getMasterMigrationsByCourseId(course1.getId()).size());
        Assertions.assertEquals(masterMigration, masterMigrationRepo.getMasterMigrationsByCourseId(course1.getId()).get(0));
    }

    @Test
    void verifyCreateMasterMigration(){
        Course course1 = courseSeeders.populatedCourse();

        Optional<MasterMigration> masterMigration = migrationService.createMasterMigration(course1.getId().toString());
        Assertions.assertTrue(masterMigration.isPresent());
        List<Migration> migrationList = migrationService.getMigrationsByMasterMigration(masterMigration.get().getId().toString());
        Assertions.assertEquals(0, migrationList.size());

    }

    @Test
    void verifyUpdatePolicy(){
        Course course1 = courseSeeders.populatedCourse();
        Optional<MasterMigration> masterMigration = migrationService.createMasterMigration(course1.getId().toString());
        Assertions.assertTrue(masterMigration.isPresent());
        Optional<Assignment> assignment = course1.getAssignments().stream().findFirst();
        Assertions.assertTrue(assignment.isPresent());

        Policy policy = new Policy();
        policy.setAssignment(assignment.get());
        policy.setPolicyName("test_policy");
        policy.setPolicyURI("http://file.js");
        policy.setCourse(course1);
        User user = userSeeders.user1();
        policy.setCreatedByUser(user);
        policyRepo.save(policy);
        migrationService.addMigration(masterMigration.get().getId().toString(), assignment.get().getId().toString(), policy.getPolicyURI());

        List<Migration> migrationList = migrationService.getMigrationsByMasterMigration(masterMigration.get().getId().toString());
        Assertions.assertEquals(1, migrationList.size());
        Assertions.assertEquals(policy.getPolicyURI(), migrationList.getFirst().getPolicy().getPolicyURI());

        Policy updatedPolicy = new Policy();
        updatedPolicy.setAssignment(assignment.get());
        updatedPolicy.setPolicyName("updated_test_policy");
        updatedPolicy.setPolicyURI("http://file2.js");
        updatedPolicy.setCourse(course1);
        updatedPolicy.setCreatedByUser(user);
        policyRepo.save(updatedPolicy);
        migrationService.updatePolicyForMigration(migrationList.get(0).getId().toString(), updatedPolicy.getPolicyURI());

        migrationList = migrationService.getMigrationsByMasterMigration(masterMigration.get().getId().toString());
        Assertions.assertEquals(1, migrationList.size());
        Assertions.assertEquals(updatedPolicy.getPolicyURI(), migrationList.getFirst().getPolicy().getPolicyURI());
    }

    @Test
    void verifyMigrationsCreated() {
        Course course1 = courseSeeders.populatedCourse();
        Optional<MasterMigration> masterMigration = migrationService.createMasterMigration(course1.getId().toString());
        Assertions.assertTrue(masterMigration.isPresent());
        Optional<Assignment> assignment = course1.getAssignments().stream().findFirst();
        Assertions.assertTrue(assignment.isPresent());

        Policy policy = new Policy();
        policy.setAssignment(assignment.get());
        policy.setPolicyName("test_policy");
        policy.setPolicyURI("http://file.js");
        policy.setCourse(course1);
        User user = userSeeders.user1();
        policy.setCreatedByUser(user);
        policyRepo.save(policy);
        migrationService.addMigration(masterMigration.get().getId().toString(), assignment.get().getId().toString(), policy.getPolicyURI());

        List<Migration> migrationList = migrationService.getMigrationsByMasterMigration(masterMigration.get().getId().toString());
        Assertions.assertEquals(1, migrationList.size());
        Assertions.assertEquals(assignment.get().getId().toString(), migrationList.getFirst().getAssignment().getId().toString());
        Assertions.assertEquals(policy.getPolicyURI(), migrationList.getFirst().getPolicy().getPolicyURI());
    }

    @Test
    @Transactional
    void verifyHandleScore(){
        User user = userSeeders.user1();

        UUID migrationId = UUID.randomUUID();

        ScoredDTO dto = new ScoredDTO();
        dto.setCwid("100000");
        dto.setFinalScore(10);
        dto.setAdjustedSubmissionTime(Instant.now());
        dto.setExtensionStatus(LateRequestStatus.NO_EXTENSION);
        dto.setSubmissionStatus(SubmissionStatus.ON_TIME);


        migrationService.handleScoreReceived(user, migrationId, dto);

        List<MigrationTransactionLog> entries = migrationTransactionLogRepo.getAllByMigrationId(migrationId);

        Assertions.assertEquals(1, entries.size());

        Assertions.assertEquals(dto.getFinalScore(), entries.getFirst().getScore());
        Assertions.assertEquals(dto.getCwid(), entries.getFirst().getCwid());
        Assertions.assertEquals(migrationId, entries.getFirst().getMigrationId());
        Assertions.assertEquals(user, entries.getFirst().getPerformedByUser());

    }

}
