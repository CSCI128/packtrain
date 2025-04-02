package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.containers.PostgresTestContainer;
import edu.mines.gradingadmin.models.*;
import edu.mines.gradingadmin.models.enums.SubmissionStatus;
import edu.mines.gradingadmin.repositories.RawScoreRepo;
import edu.mines.gradingadmin.seeders.AssignmentSeeder;
import edu.mines.gradingadmin.seeders.CourseSeeders;
import edu.mines.gradingadmin.seeders.MigrationSeeder;
import edu.mines.gradingadmin.seeders.UserSeeders;
import jakarta.transaction.Transactional;
import lombok.SneakyThrows;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@SpringBootTest
@Transactional
public class TestRawScoreService implements PostgresTestContainer {

    @Autowired
    private RawScoreRepo rawScoreRepo;

    @Autowired
    private RawScoreService rawScoreService;
    @Autowired
    private UserSeeders userSeeders;
    @Autowired
    private CourseSeeders courseSeeders;
    @Autowired
    private AssignmentSeeder assignmentSeeder;
    @Autowired
    private MigrationSeeder migrationSeeder;

    private Assignment assignment;
    private MasterMigration masterMigration;

    @BeforeAll
    static void setupClass(){
        postgres.start();
    }

    @AfterEach
    void tearDown(){
        migrationSeeder.clearAll();
        assignmentSeeder.clearAll();
        courseSeeders.clear();
        rawScoreRepo.deleteAll();
        userSeeders.clearAll();
    }

    @BeforeEach
    void beforeEach(){
        User user = userSeeders.user1();
        Course course = courseSeeders.course1();
        assignment = assignmentSeeder.worksheet1(course);
        masterMigration = migrationSeeder.masterMigration(course, user);
    }

    @Test
    @SneakyThrows
    void testEmpty(){
        Migration migration = migrationSeeder.migration(assignment, masterMigration);
        String fileContent = "";

        String filename = "test.csv";
        MockMultipartFile file = new MockMultipartFile(filename, filename, "text/csv", fileContent.getBytes());

        List<RawScore> rawScores = rawScoreService.uploadGradescopeCSV(file.getInputStream(), migration.getId());

        Assertions.assertTrue(rawScores.isEmpty());
    }

    @Test
    @SneakyThrows
    void testSkipFirst(){
        Migration migration = migrationSeeder.migration(assignment, masterMigration);
        String fileContent = "First Name,Last Name,SID,,,,Total Score,Max Points,Status,,Submission Time,Lateness (H:M:S)\n";

        String filename = "test.csv";
        MockMultipartFile file = new MockMultipartFile(filename, filename, "text/csv", fileContent.getBytes());

        List<RawScore> rawScores = rawScoreService.uploadGradescopeCSV(file.getInputStream(), migration.getId());

        Assertions.assertTrue(rawScores.isEmpty());
    }

    @Test
    @SneakyThrows
    void testParse(){
        Migration migration = migrationSeeder.migration(assignment, masterMigration);
        String fileContent = "First Name,Last Name,SID,,,,Total Score,Max Points,Status,,Submission Time,Lateness (H:M:S)\n" +
                "Jane,Doe,12344321,,,,12.0,12.0,Graded,,2022-06-25 13:16:26 -0600,13:29:30\n" +
                "Tester,Testing,testtest,,,,12.0,12.0,Graded,,2022-06-25 13:16:58 -0600,00:00:00\n" +
                "Jimmy,yyy,jimmyyyy,,,,11.5,12.0,Graded,,2022-06-25 13:25:12 -0600,07:32:50\n" +
                "Joe,Jam,121212,,,,,12.0,Missing\n";

        String filename = "test.csv";
        MockMultipartFile file = new MockMultipartFile(filename, filename, "text/csv", fileContent.getBytes());

        // Should add scores to the list that were properly saved
        List<RawScore> scores = rawScoreService.uploadGradescopeCSV(file.getInputStream(), migration.getId());

        Assertions.assertFalse(scores.isEmpty());

        // Singular test get on saved score
        Optional<RawScore> score = rawScoreService.getRawScoreForCwidAndMigrationId("12344321", migration.getId());
        Assertions.assertFalse(score.isEmpty());
        Assertions.assertEquals(SubmissionStatus.LATE, score.get().getSubmissionStatus());

        // Test weird conversion fields
        score = rawScoreService.getRawScoreForCwidAndMigrationId("121212", migration.getId());
        Assertions.assertNull(score.get().getScore());
        Assertions.assertEquals(SubmissionStatus.MISSING, score.get().getSubmissionStatus());
        Assertions.assertNull(score.get().getHoursLate());
        Assertions.assertNull(score.get().getSubmissionTime());

        // Test weird conversion fields
        score = rawScoreService.getRawScoreForCwidAndMigrationId("jimmyyyy", migration.getId());
        Assertions.assertEquals(11.5, score.get().getScore());
        Assertions.assertEquals(7.5472, score.get().getHoursLate(), 0.001);

        LocalDateTime localDateTime = LocalDateTime.of(2022, 6, 25, 13, 25, 12);
        ZoneOffset zoneOffset = ZoneOffset.of("-06:00");
        Instant instant = localDateTime.toInstant(zoneOffset);

        Assertions.assertEquals(instant, score.get().getSubmissionTime());

    }

    @Test
    @SneakyThrows
    void testAllGraded(){
        Migration migration = migrationSeeder.migration(assignment, masterMigration);
        String fileContent = """
                First Name,Last Name,SID,Email,Sections,section_name,Total Score,Max Points,Status,Submission ID,Submission Time,Lateness (H:M:S)
                Samual,Mcsam,101,samualmcsam@mines.edu,,,8.0,12.0,Graded,128746829,2022-06-25 13:16:26 -0600,00:00:00
                Robert,Bob,robbob,robbob@mines.edu,,,6.0,12.0,Graded,128746844,2022-06-25 13:16:58 -0600,00:00:00
                Null,IdontNull,abcdefg,nullnullnull@mymail.mines.edu,,,12.0,12.0,Graded,128746851,2022-06-25 13:17:12 -0600,02:00:00""";

        String filename = "test.csv";
        MockMultipartFile file = new MockMultipartFile(filename, filename, "text/csv", fileContent.getBytes());

        List<RawScore> rawScoreList = rawScoreService.uploadGradescopeCSV(file.getInputStream(), migration.getId());

        Assertions.assertEquals("101", rawScoreList.get(0).getCwid());
        Assertions.assertEquals("robbob", rawScoreList.get(1).getCwid());
        Assertions.assertEquals("abcdefg", rawScoreList.get(2).getCwid());

        LocalDateTime localDateTime = LocalDateTime.of(2022, 6, 25, 13, 16, 26);
        ZoneOffset zoneOffset = ZoneOffset.of("-06:00");
        Instant instant = localDateTime.toInstant(zoneOffset);

        Assertions.assertEquals(instant, rawScoreList.get(0).getSubmissionTime());

        localDateTime = LocalDateTime.of(2022, 6, 25, 13, 16, 58);
        instant = localDateTime.toInstant(zoneOffset);

        Assertions.assertEquals(instant, rawScoreList.get(1).getSubmissionTime());

        localDateTime = LocalDateTime.of(2022, 6, 25, 13, 17, 12);
        instant = localDateTime.toInstant(zoneOffset);

        Assertions.assertEquals(instant, rawScoreList.get(2).getSubmissionTime());

        Assertions.assertNotNull(rawScoreList.get(0));
        Assertions.assertNotNull(rawScoreList.get(1));
        Assertions.assertNotNull(rawScoreList.get(2));

        Assertions.assertEquals(8.0, rawScoreList.get(0).getScore());
        Assertions.assertEquals(6.0, rawScoreList.get(1).getScore());
        Assertions.assertEquals(12.0, rawScoreList.get(2).getScore());

        Assertions.assertEquals(SubmissionStatus.ON_TIME, rawScoreList.get(0).getSubmissionStatus());
        Assertions.assertEquals(SubmissionStatus.ON_TIME, rawScoreList.get(1).getSubmissionStatus());
        Assertions.assertEquals(SubmissionStatus.LATE, rawScoreList.get(2).getSubmissionStatus());

        Assertions.assertEquals(0, rawScoreList.get(0).getHoursLate());
        Assertions.assertEquals(0, rawScoreList.get(1).getHoursLate());
        Assertions.assertEquals(2, rawScoreList.get(2).getHoursLate());

    }

    @Test
    @SneakyThrows
    void testAllUngraded(){
        Migration migration = migrationSeeder.migration(assignment, masterMigration);
        String fileContent = """
                First Name,Last Name,SID,Email,Sections,section_name,Total Score,Max Points,Status,Submission ID,Submission Time,Lateness (H:M:S)
                Samual,Mcsam,101,samualmcsam@mines.edu,,,6.0,12.0,Ungraded,128746829,2022-06-25 13:16:26 -0600,00:00:00
                Robert,Bob,robbob,robbob@mines.edu,,,4.0,12.0,Ungraded,128746844,2022-06-25 13:16:58 -0600,00:00:00
                Null,IdontNull,abcdefg,nullnullnull@mymail.mines.edu,,,0.0,12.0,Ungraded,128746851,2022-06-25 13:17:12 -0600,02:00:00""";

        String filename = "test.csv";
        MockMultipartFile file = new MockMultipartFile(filename, filename, "text/csv", fileContent.getBytes());

        List<RawScore> rawScoreList = rawScoreService.uploadGradescopeCSV(file.getInputStream(), migration.getId());

        Assertions.assertEquals("101", rawScoreList.get(0).getCwid());
        Assertions.assertEquals("robbob", rawScoreList.get(1).getCwid());
        Assertions.assertEquals("abcdefg", rawScoreList.get(2).getCwid());

        LocalDateTime localDateTime = LocalDateTime.of(2022, 6, 25, 13, 16, 26);
        ZoneOffset zoneOffset = ZoneOffset.of("-06:00");
        Instant instant = localDateTime.toInstant(zoneOffset);

        Assertions.assertEquals(instant, rawScoreList.get(0).getSubmissionTime());

        localDateTime = LocalDateTime.of(2022, 6, 25, 13, 16, 58);
        instant = localDateTime.toInstant(zoneOffset);

        Assertions.assertEquals(instant, rawScoreList.get(1).getSubmissionTime());

        localDateTime = LocalDateTime.of(2022, 6, 25, 13, 17, 12);
        instant = localDateTime.toInstant(zoneOffset);

        Assertions.assertEquals(instant, rawScoreList.get(2).getSubmissionTime());

        Assertions.assertNotNull(rawScoreList.get(0));
        Assertions.assertNotNull(rawScoreList.get(1));
        Assertions.assertNotNull(rawScoreList.get(2));

        Assertions.assertEquals(6.0, rawScoreList.get(0).getScore());
        Assertions.assertEquals(4.0, rawScoreList.get(1).getScore());
        Assertions.assertEquals(0.0, rawScoreList.get(2).getScore());

        Assertions.assertEquals(SubmissionStatus.ON_TIME, rawScoreList.get(0).getSubmissionStatus());
        Assertions.assertEquals(SubmissionStatus.ON_TIME, rawScoreList.get(1).getSubmissionStatus());
        Assertions.assertEquals(SubmissionStatus.LATE, rawScoreList.get(2).getSubmissionStatus());

        Assertions.assertEquals(0, rawScoreList.get(0).getHoursLate());
        Assertions.assertEquals(0, rawScoreList.get(1).getHoursLate());
        Assertions.assertEquals(2, rawScoreList.get(2).getHoursLate());

    }

    @Test
    @SneakyThrows
    void testAllMissing() {
        Migration migration = migrationSeeder.migration(assignment, masterMigration);
        String fileContent = """
                First Name,Last Name,SID,Email,Sections,section_name,Total Score,Max Points,Status,Submission ID,Submission Time,Lateness (H:M:S)
                Samual,Mcsam,101,samualmcsam@mines.edu,,,,12.0,Missing,128746829,,
                Robert,Bob,robbob,robbob@mines.edu,,,,12.0,Missing,128746844,,
                Null,IdontNull,abcdefg,nullnullnull@mymail.mines.edu,,,,12.0,Missing,128746851,,""";

        String filename = "test.csv";
        MockMultipartFile file = new MockMultipartFile(filename, filename, "text/csv", fileContent.getBytes());

        List<RawScore> rawScoreList = rawScoreService.uploadGradescopeCSV(file.getInputStream(), migration.getId());

        Assertions.assertEquals("101", rawScoreList.get(0).getCwid());
        Assertions.assertEquals("robbob", rawScoreList.get(1).getCwid());
        Assertions.assertEquals("abcdefg", rawScoreList.get(2).getCwid());

        Assertions.assertNull(rawScoreList.get(0).getSubmissionTime());
        Assertions.assertNull(rawScoreList.get(1).getSubmissionTime());
        Assertions.assertNull(rawScoreList.get(2).getSubmissionTime());

        Assertions.assertNull(rawScoreList.get(0).getScore());
        Assertions.assertNull(rawScoreList.get(1).getScore());
        Assertions.assertNull(rawScoreList.get(2).getScore());

        Assertions.assertNull(rawScoreList.get(0).getHoursLate());
        Assertions.assertNull(rawScoreList.get(1).getHoursLate());
        Assertions.assertNull(rawScoreList.get(2).getHoursLate());

        Assertions.assertEquals(SubmissionStatus.MISSING, rawScoreList.get(0).getSubmissionStatus());
        Assertions.assertEquals(SubmissionStatus.MISSING, rawScoreList.get(1).getSubmissionStatus());
        Assertions.assertEquals(SubmissionStatus.MISSING, rawScoreList.get(2).getSubmissionStatus());

        Assertions.assertNotNull(rawScoreList.get(0));
        Assertions.assertNotNull(rawScoreList.get(1));
        Assertions.assertNotNull(rawScoreList.get(2));
    }

}