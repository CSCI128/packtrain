package edu.mines.gradingadmin.services;


import edu.mines.gradingadmin.containers.PostgresTestContainer;
import edu.mines.gradingadmin.data.policyServer.ScoredDTO;
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
import org.springframework.web.server.ResponseStatusException;

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
    private RawScoreRepo rawScoreRepo;
    private Course course;
    private User user;
    @Autowired
    private PolicyService policyService;

    @BeforeAll
    static void setupClass(){
        postgres.start();

    }

    @BeforeEach
    void setup(){
        migrationService = new MigrationService(migrationRepo, masterMigrationRepo, migrationTransactionLogRepo, taskRepo,
                extensionService, courseService, assignmentService, Mockito.mock(ApplicationEventPublisher.class),
                Mockito.mock(RabbitMqService.class), Mockito.mock(PolicyServerService.class), rawScoreRepo, masterMigrationStatsRepo, policyService);

        course = courseSeeders.populatedCourse();
        user = userSeeders.user1();

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
    void verifyCreateMasterMigration(){
        Optional<MasterMigration> masterMigration = migrationService.createMasterMigration(course.getId().toString(), user);
        Assertions.assertTrue(masterMigration.isPresent());
        List<Migration> migrationList = migrationService.getMigrationsByMasterMigration(masterMigration.get().getId().toString());
        Assertions.assertEquals(0, migrationList.size());

    }

    @Test
    void verifyUpdatePolicy(){
        Optional<MasterMigration> masterMigration = migrationService.createMasterMigration(course.getId().toString(), user);
        Assertions.assertTrue(masterMigration.isPresent());
        Optional<Assignment> assignment = course.getAssignments().stream().findFirst();
        Assertions.assertTrue(assignment.isPresent());

        Policy policy = new Policy();
        policy.setPolicyName("test_policy");
        policy.setPolicyURI("http://file.js");
        policy.setFileName("file.js");
        policy.setCourse(course);
        User user = userSeeders.user1();
        policy.setCreatedByUser(user);
        policyRepo.save(policy);
        migrationService.addMigration(masterMigration.get().getId().toString(), assignment.get().getId().toString(), policy.getPolicyURI());

        List<Migration> migrationList = migrationService.getMigrationsByMasterMigration(masterMigration.get().getId().toString());
        Assertions.assertEquals(1, migrationList.size());
        Assertions.assertEquals(policy.getPolicyURI(), migrationList.getFirst().getPolicy().getPolicyURI());

        Policy updatedPolicy = new Policy();
        updatedPolicy.setPolicyName("updated_test_policy");
        updatedPolicy.setPolicyURI("http://file2.js");
        updatedPolicy.setFileName("file2.js");
        updatedPolicy.setCourse(course);
        updatedPolicy.setCreatedByUser(user);
        policyRepo.save(updatedPolicy);
        migrationService.updatePolicyForMigration(migrationList.get(0).getId().toString(), updatedPolicy.getPolicyURI());

        migrationList = migrationService.getMigrationsByMasterMigration(masterMigration.get().getId().toString());
        Assertions.assertEquals(1, migrationList.size());
        Assertions.assertEquals(updatedPolicy.getPolicyURI(), migrationList.getFirst().getPolicy().getPolicyURI());
    }

    @Test
    void verifyMigrationsCreated() {
        Optional<MasterMigration> masterMigration = migrationService.createMasterMigration(course.getId().toString(), user);
        Assertions.assertTrue(masterMigration.isPresent());
        Optional<Assignment> assignment = course.getAssignments().stream().findFirst();
        Assertions.assertTrue(assignment.isPresent());

        Policy policy = new Policy();
        policy.setPolicyName("test_policy");
        policy.setPolicyURI("http://file.js");
        policy.setFileName("file.js");
        policy.setCourse(course);
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