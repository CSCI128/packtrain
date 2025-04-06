package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.containers.PostgresTestContainer;
import edu.mines.gradingadmin.models.*;
import edu.mines.gradingadmin.models.enums.SubmissionStatus;
import edu.mines.gradingadmin.repositories.RawScoreRepo;
import edu.mines.gradingadmin.seeders.*;
import jakarta.transaction.Transactional;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

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
    @Autowired
    private CourseMemberSeeder courseMemberSeeder;

    private Assignment assignment;
    private MasterMigration masterMigration;
    private Course course;

    @BeforeAll
    static void setupClass(){
        postgres.start();
    }

    @BeforeEach
    void beforeEach(){
        User user = userSeeders.user1();
        course = courseSeeders.course1();
        assignment = assignmentSeeder.worksheet1(course);
        masterMigration = migrationSeeder.masterMigration(course, user);
    }

    @AfterEach
    void tearDown(){
        migrationSeeder.clearAll();
        assignmentSeeder.clearAll();
        courseMemberSeeder.clearAll();
        courseSeeders.clear();
        rawScoreRepo.deleteAll();
        userSeeders.clearAll();
    }

    private static MockMultipartFile getGradescopeGradesheet() {
        String fileContent = """
First Name,Last Name,SID,,,,Total Score,Max Points,Status,,Submission Time,Lateness (H:M:S)
Jane,Doe,12344321,,,,12.0,12.0,Graded,,2022-06-25 13:16:26 -0600,13:29:30
Tester,Testing,testtest,,,,12.0,12.0,Graded,,2022-06-25 13:16:58 -0600,00:00:00
Jimmy,yyy,jimmyyyy,,,,11.5,12.0,Ungraded,,2022-06-25 13:25:12 -0600,07:32:50
Joe,Jam,121212,,,,,12.0,Missing
                """;

        String filename = "test.csv";
        return new MockMultipartFile(filename, filename, "text/csv", fileContent.getBytes());
    }

    private MockMultipartFile getPrairieLearnGradesheetGroup(){
        String fileContent = """
Group name,Usernames,Score,Question points,Max points,Question % score,Auto points,Max auto points,Manual points,Max manual points,Submission date
Group A,"[""alice@example.com"", ""bob@example.com""]",5,10,10,50,5,10,0,0,2020-01-22T00:00:01-06
Group A,"[""alice@example.com"", ""bob@example.com""]",5,10,10,50,5,10,0,0,2020-01-22T00:00:01-06
Group B,"[""charlie@example.com"", ""david@example.com""]",5,10,10,50,5,10,0,0,2020-02-01T00:00:01-06
Group B,"[""charlie@example.com"", ""david@example.com""]",5,10,10,50,5,10,0,0,2020-02-01T00:00:01-06
Group C,"[""eve@example.com"", ""frank@example.com""]",5,10,10,50,5,10,0,0,2020-01-22T00:00:01-06
                """;

        Section s = courseSeeders.section(course);
        List.of(
                userSeeders.user("Alice", "alice@example.com", "alice"),
                userSeeders.user("Bob", "bob@example.com", "bob"),
                userSeeders.user("Charlie", "charlie@example.com", "charlie"),
                userSeeders.user("David", "david@example.com", "david"),
                userSeeders.user("Eve", "eve@example.com", "eve"),
                userSeeders.user("Frank", "frank@example.com", "frank")
        ).forEach(u -> courseMemberSeeder.student(u, course, s));

        String filename = "sheet.csv";
        return new MockMultipartFile(filename, filename, "text/csv", fileContent.getBytes());
    }

    @Test
    @SneakyThrows
    void testEmptyPL(){
        Migration migration = migrationSeeder.migration(assignment, masterMigration);
        String fileContent = "";

        String filename = "test.csv";
        MockMultipartFile file = new MockMultipartFile(filename, filename, "text/csv", fileContent.getBytes());

        rawScoreService.uploadPrairieLearnCSV(file.getInputStream(), migration.getId());
        List<RawScore> rawScores = rawScoreService.getRawScoresFromMigration(migration.getId());

        Assertions.assertTrue(rawScores.isEmpty());
    }

    @Test
    @SneakyThrows
    void testParsePLGroupsOnTime(){
        Migration migration = migrationSeeder.migration(assignment, masterMigration);

        MockMultipartFile file = getPrairieLearnGradesheetGroup();

        rawScoreService.uploadPrairieLearnCSV(file.getInputStream(), migration.getId());
        List<RawScore> rawScores = rawScoreService.getRawScoresFromMigration(migration.getId());

        Assertions.assertEquals(6, rawScores.size());

        Optional<RawScore> aliceSummedScore = rawScoreService.getRawScoreForCwidAndMigration("alice", migration.getId());
        Assertions.assertTrue(aliceSummedScore.isPresent());
        Assertions.assertEquals(20, aliceSummedScore.get().getScore());
        Assertions.assertEquals(SubmissionStatus.ON_TIME, aliceSummedScore.get().getSubmissionStatus());
        Assertions.assertEquals(0, aliceSummedScore.get().getHoursLate().intValue());
    }

    @Test
    @SneakyThrows
    void testParsePLGroupsLate(){
        Migration migration = migrationSeeder.migration(assignment, masterMigration);

        MockMultipartFile file = getPrairieLearnGradesheetGroup();

        rawScoreService.uploadPrairieLearnCSV(file.getInputStream(), migration.getId());
        List<RawScore> rawScores = rawScoreService.getRawScoresFromMigration(migration.getId());

        Assertions.assertEquals(6, rawScores.size());

        Optional<RawScore> charlie = rawScoreService.getRawScoreForCwidAndMigration("charlie", migration.getId());
        Assertions.assertTrue(charlie.isPresent());
        Assertions.assertEquals(20, charlie.get().getScore());
        Assertions.assertEquals(SubmissionStatus.LATE, charlie.get().getSubmissionStatus());
        Assertions.assertEquals(30, charlie.get().getHoursLate().intValue());
    }



    @Test
    @SneakyThrows
    void testEmptyGS(){
        Migration migration = migrationSeeder.migration(assignment, masterMigration);
        String fileContent = "";

        String filename = "test.csv";
        MockMultipartFile file = new MockMultipartFile(filename, filename, "text/csv", fileContent.getBytes());

        rawScoreService.uploadGradescopeCSV(file.getInputStream(), migration.getId());
        List<RawScore> rawScores = rawScoreService.getRawScoresFromMigration(migration.getId());

        Assertions.assertTrue(rawScores.isEmpty());
    }

    @Test
    @SneakyThrows
    void testParseGSGraded(){
        Migration migration = migrationSeeder.migration(assignment, masterMigration);
        MockMultipartFile file = getGradescopeGradesheet();

        // Should add scores to the list that were properly saved
        rawScoreService.uploadGradescopeCSV(file.getInputStream(), migration.getId());
        List<RawScore> rawScores = rawScoreService.getRawScoresFromMigration(migration.getId());

        Assertions.assertEquals(3, rawScores.size());

        // Singular test get on saved score
        Optional<RawScore> score = rawScoreService.getRawScoreForCwidAndMigration("12344321", migration.getId());
        Assertions.assertTrue(score.isPresent());
        Assertions.assertEquals(SubmissionStatus.LATE, score.get().getSubmissionStatus());
    }

    @Test
    @SneakyThrows
    void testParseGSMissing(){
        Migration migration = migrationSeeder.migration(assignment, masterMigration);
        MockMultipartFile file = getGradescopeGradesheet();

        // Should add scores to the list that were properly saved
        rawScoreService.uploadGradescopeCSV(file.getInputStream(), migration.getId());
        List<RawScore> rawScores = rawScoreService.getRawScoresFromMigration(migration.getId());

        Assertions.assertEquals(3, rawScores.size());

        Optional<RawScore> score = rawScoreService.getRawScoreForCwidAndMigration("121212", migration.getId());
        Assertions.assertTrue(score.isPresent());
        Assertions.assertNull(score.get().getScore());
        Assertions.assertEquals(SubmissionStatus.MISSING, score.get().getSubmissionStatus());
        Assertions.assertNull(score.get().getHoursLate());
        Assertions.assertNull(score.get().getSubmissionTime());
    }

    @Test
    @SneakyThrows
    void testParseGSUngraded(){
        Migration migration = migrationSeeder.migration(assignment, masterMigration);
        MockMultipartFile file = getGradescopeGradesheet();

        // Should add scores to the list that were properly saved
        rawScoreService.uploadGradescopeCSV(file.getInputStream(), migration.getId());
        List<RawScore> rawScores = rawScoreService.getRawScoresFromMigration(migration.getId());

        Assertions.assertEquals(3, rawScores.size());

        Optional<RawScore> score = rawScoreService.getRawScoreForCwidAndMigration("jimmyyyy", migration.getId());
        Assertions.assertTrue(score.isEmpty());
    }


}