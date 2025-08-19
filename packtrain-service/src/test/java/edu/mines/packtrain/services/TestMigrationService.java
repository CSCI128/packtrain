package edu.mines.packtrain.services;


import edu.mines.packtrain.containers.PostgresTestContainer;
import edu.mines.packtrain.data.policyServer.ScoredDTO;
import edu.mines.packtrain.managers.ImpersonationManager;
import edu.mines.packtrain.models.*;
import edu.mines.packtrain.models.enums.LateRequestStatus;
import edu.mines.packtrain.models.enums.SubmissionStatus;
import edu.mines.packtrain.models.tasks.PostToCanvasTaskDef;
import edu.mines.packtrain.models.tasks.ProcessScoresAndExtensionsTaskDef;
import edu.mines.packtrain.models.tasks.ZeroOutSubmissionsTaskDef;
import edu.mines.packtrain.repositories.*;
import edu.mines.packtrain.seeders.AssignmentSeeder;
import edu.mines.packtrain.seeders.CourseSeeders;
import edu.mines.packtrain.seeders.MigrationSeeder;
import edu.mines.packtrain.seeders.UserSeeders;
import edu.mines.packtrain.services.external.CanvasService;
import edu.mines.packtrain.services.external.PolicyServerService;
import edu.mines.packtrain.services.external.RabbitMqService;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

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
    private ScheduledTaskRepo<ZeroOutSubmissionsTaskDef> zeroOutSubmissionTaskRepo;
    @Autowired
    private ScheduledTaskRepo<PostToCanvasTaskDef> postToCanvasTaskTaskRepo;
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
    @Autowired
    private CourseMemberService courseMemberService;
    @Autowired
    private MigrationSeeder migrationSeeder;
    @Autowired
    private AssignmentSeeder assignmentSeeder;

    @BeforeAll
    static void setupClass(){
        postgres.start();

    }

    @BeforeEach
    void setup(){
        migrationService = new MigrationService(migrationRepo, masterMigrationRepo, migrationTransactionLogRepo, taskRepo, zeroOutSubmissionTaskRepo, postToCanvasTaskTaskRepo,
                extensionService, courseService, assignmentService, Mockito.mock(ApplicationEventPublisher.class),
                Mockito.mock(RabbitMqService.class), Mockito.mock(PolicyServerService.class), rawScoreRepo, masterMigrationStatsRepo, policyService, courseMemberService, Mockito.mock(ImpersonationManager.class), Mockito.mock(CanvasService.class));

        course = courseSeeders.populatedCourse();
        user = userSeeders.user1();

    }

    @AfterEach
    void tearDown(){
        migrationSeeder.clearAll();
        policyRepo.deleteAll();
        assignmentSeeder.clearAll();
        courseSeeders.clearAll();
        userSeeders.clearAll();
    }

    @Test
    void verifyCreateMasterMigration(){
        MasterMigration masterMigration = migrationService.createMasterMigration(course.getId(), user);
        List<Migration> migrationList = migrationService.getMigrationsByMasterMigration(masterMigration.getId());
        Assertions.assertEquals(0, migrationList.size());

    }

    @Test
    void verifyUpdatePolicy(){
        MasterMigration masterMigration = migrationService.createMasterMigration(course.getId(), user);
        Optional<Assignment> assignment = course.getAssignments().stream().findFirst();
        Assertions.assertTrue(assignment.isPresent());

        Policy policy = new Policy();
        policy.setPolicyName("test_policy");
        policy.setPolicyURI("http://file.js");
        policy.setFileName("file.js");
        policy.setCourse(course);
        User user = userSeeders.user1();
        policy.setCreatedByUser(user);
        policy = policyRepo.save(policy);
        migrationService.addMigration(masterMigration.getId(), assignment.get().getId());

        List<Migration> migrationList = migrationService.getMigrationsByMasterMigration(masterMigration.getId());
        Assertions.assertEquals(1, migrationList.size());

        migrationService.setPolicyForMigration(migrationList.get(0).getId(), policy.getId());

        policy = policyRepo.getPolicyById(policy.getId()).orElseThrow(AssertionError::new);

        migrationList = migrationService.getMigrationsByMasterMigration(masterMigration.getId());
        Assertions.assertEquals(1, migrationList.size());
        Assertions.assertEquals(policy.getPolicyURI(), migrationList.getFirst().getPolicy().getPolicyURI());
        Assertions.assertEquals(1, policy.getNumberOfMigrations());
    }

    @Test
    void verifyMigrationsCreated() {
        MasterMigration masterMigration = migrationService.createMasterMigration(course.getId(), user);
        Optional<Assignment> assignment = course.getAssignments().stream().findFirst();
        Assertions.assertTrue(assignment.isPresent());

        migrationService.addMigration(masterMigration.getId(), assignment.get().getId());

        List<Migration> migrationList = migrationService.getMigrationsByMasterMigration(masterMigration.getId());
        Assertions.assertEquals(1, migrationList.size());
        Assertions.assertEquals(assignment.get().getId(), migrationList.getFirst().getAssignment().getId());
    }

    @Test
    void verifyHandleScore(){
        Assignment assignment = assignmentSeeder.worksheet1(course);

        MasterMigration masterMigration = migrationSeeder.masterMigration(course, user);

        Migration migration = migrationSeeder.migration(assignment, masterMigration);

        ScoredDTO dto = new ScoredDTO();
        dto.setCwid("100000");
        dto.setFinalScore(10);
        dto.setAdjustedSubmissionTime(Instant.now());
        dto.setExtensionStatus(LateRequestStatus.NO_EXTENSION);
        dto.setSubmissionStatus(SubmissionStatus.ON_TIME);

        ResponseStatusException exception = Assertions.assertThrows(
                ResponseStatusException.class,
                () -> migrationService.handleScoreReceived(user, migration.getId(), dto)
        );

        Assertions.assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }
}