package edu.mines.gradingadmin.services;


import edu.mines.gradingadmin.containers.PostgresTestContainer;
import edu.mines.gradingadmin.data.policyServer.ScoredDTO;
import edu.mines.gradingadmin.managers.ImpersonationManager;
import edu.mines.gradingadmin.models.*;
import edu.mines.gradingadmin.models.enums.LateRequestStatus;
import edu.mines.gradingadmin.models.enums.SubmissionStatus;
import edu.mines.gradingadmin.models.tasks.PostToCanvasTaskDef;
import edu.mines.gradingadmin.models.tasks.ProcessScoresAndExtensionsTaskDef;
import edu.mines.gradingadmin.models.tasks.ZeroOutSubmissionsTaskDef;
import edu.mines.gradingadmin.repositories.*;
import edu.mines.gradingadmin.seeders.AssignmentSeeder;
import edu.mines.gradingadmin.seeders.CourseSeeders;
import edu.mines.gradingadmin.seeders.MigrationSeeder;
import edu.mines.gradingadmin.seeders.UserSeeders;
import edu.mines.gradingadmin.services.external.CanvasService;
import edu.mines.gradingadmin.services.external.PolicyServerService;
import edu.mines.gradingadmin.services.external.RabbitMqService;
import jakarta.transaction.Transactional;
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
        MasterMigration masterMigration = migrationService.createMasterMigration(course.getId().toString(), user);
        List<Migration> migrationList = migrationService.getMigrationsByMasterMigration(masterMigration.getId().toString());
        Assertions.assertEquals(0, migrationList.size());

    }

    @Test
    void verifyUpdatePolicy(){
        MasterMigration masterMigration = migrationService.createMasterMigration(course.getId().toString(), user);
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
        migrationService.addMigration(masterMigration.getId().toString(), assignment.get().getId().toString());

        List<Migration> migrationList = migrationService.getMigrationsByMasterMigration(masterMigration.getId().toString());
        Assertions.assertEquals(1, migrationList.size());

        migrationService.setPolicyForMigration(migrationList.get(0).getId().toString(), policy.getId().toString());

        policy = policyRepo.getPolicyById(policy.getId()).orElseThrow(AssertionError::new);

        migrationList = migrationService.getMigrationsByMasterMigration(masterMigration.getId().toString());
        Assertions.assertEquals(1, migrationList.size());
        Assertions.assertEquals(policy.getPolicyURI(), migrationList.getFirst().getPolicy().getPolicyURI());
        Assertions.assertEquals(1, policy.getNumberOfMigrations());
    }

    @Test
    void verifyMigrationsCreated() {
        MasterMigration masterMigration = migrationService.createMasterMigration(course.getId().toString(), user);
        Optional<Assignment> assignment = course.getAssignments().stream().findFirst();
        Assertions.assertTrue(assignment.isPresent());

        migrationService.addMigration(masterMigration.getId().toString(), assignment.get().getId().toString());

        List<Migration> migrationList = migrationService.getMigrationsByMasterMigration(masterMigration.getId().toString());
        Assertions.assertEquals(1, migrationList.size());
        Assertions.assertEquals(assignment.get().getId().toString(), migrationList.getFirst().getAssignment().getId().toString());
    }

    @Test
    void verifyHandleScore(){
        Assignment assignment =  assignmentSeeder.worksheet1(course);

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