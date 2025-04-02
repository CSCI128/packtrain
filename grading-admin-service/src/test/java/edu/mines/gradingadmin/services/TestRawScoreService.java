package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.containers.PostgresTestContainer;
import edu.mines.gradingadmin.models.RawScore;
import edu.mines.gradingadmin.models.enums.SubmissionStatus;
import edu.mines.gradingadmin.repositories.RawScoreRepo;
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

    @BeforeAll
    static void setupClass(){
        postgres.start();
    }

    @AfterEach
    void tearDown(){
        rawScoreRepo.deleteAll();
    }

    @Test
    @SneakyThrows
    void testEmpty(){
        String fileContent = "";

        String filename = "test.csv";
        MockMultipartFile file = new MockMultipartFile(filename, filename, "text/csv", fileContent.getBytes());
        UUID testId = UUID.randomUUID();

        List<RawScore> rawScores = rawScoreService.uploadGradescopeCSV(file.getInputStream(), testId);

        Assertions.assertTrue(rawScores.isEmpty());
    }

    @Test
    @SneakyThrows
    void testSkipFirst(){
        String fileContent = "First Name,Last Name,SID,,,,Total Score,Max Points,Status,,Submission Time,Lateness (H:M:S)\n";

        String filename = "test.csv";
        MockMultipartFile file = new MockMultipartFile(filename, filename, "text/csv", fileContent.getBytes());
        UUID testId = UUID.randomUUID();

        List<RawScore> rawScores = rawScoreService.uploadGradescopeCSV(file.getInputStream(), testId);

        Assertions.assertTrue(rawScores.isEmpty());
    }

    @Test
    @SneakyThrows
    void testParse(){

        String fileContent = "First Name,Last Name,SID,,,,Total Score,Max Points,Status,,Submission Time,Lateness (H:M:S)\n" +
                "Jane,Doe,12344321,,,,12.0,12.0,Graded,,2022-06-25 13:16:26 -0600,13:29:30\n" +
                "Tester,Testing,testtest,,,,12.0,12.0,Graded,,2022-06-25 13:16:58 -0600,00:00:00\n" +
                "Jimmy,yyy,jimmyyyy,,,,11.5,12.0,Graded,,2022-06-25 13:25:12 -0600,07:32:50\n" +
                "Joe,Jam,121212,,,,,12.0,Missing\n";

        String filename = "test.csv";
        MockMultipartFile file = new MockMultipartFile(filename, filename, "text/csv", fileContent.getBytes());
        UUID testId = UUID.randomUUID();

        // Should add scores to the list that were properly saved
        List<RawScore> scores = rawScoreService.uploadGradescopeCSV(file.getInputStream(), testId);

        Assertions.assertFalse(scores.isEmpty());

        // Singular test get on saved score
        Optional<RawScore> score = rawScoreService.getRawScoreForCwidAndMigrationId("12344321", testId);
        Assertions.assertFalse(score.isEmpty());
        Assertions.assertEquals(SubmissionStatus.LATE, score.get().getSubmissionStatus());

        // Test weird conversion fields
        score = rawScoreService.getRawScoreForCwidAndMigrationId("121212", testId);
        Assertions.assertNull(score.get().getScore());
        Assertions.assertEquals(SubmissionStatus.MISSING, score.get().getSubmissionStatus());
        Assertions.assertNull(score.get().getHoursLate());
        Assertions.assertNull(score.get().getSubmissionTime());

        // Test weird conversion fields
        score = rawScoreService.getRawScoreForCwidAndMigrationId("jimmyyyy", testId);
        Assertions.assertEquals(11.5, score.get().getScore());
        Assertions.assertEquals(7.5472, score.get().getHoursLate(), 0.001);

        LocalDateTime localDateTime = LocalDateTime.of(2022, 6, 25, 13, 25, 12);
        ZoneOffset zoneOffset = ZoneOffset.of("-06:00");
        Instant instant = localDateTime.toInstant(zoneOffset);

        Assertions.assertEquals(instant, score.get().getSubmissionTime());

    }

    @Test
    @SneakyThrows
    void testOverwrite(){
        String fileContent = "First Name,Last Name,SID,,,,Total Score,Max Points,Status,,Submission Time,Lateness (H:M:S)\n" +
                "Jane,Doe,12344321,,,,0.0,12.0,Graded,,2022-06-25 13:16:26 -0600,13:29:30\n";

        String filename = "test.csv";
        MockMultipartFile file = new MockMultipartFile(filename, filename, "text/csv", fileContent.getBytes());
        UUID testId = UUID.randomUUID();

        RawScore firstRawScore = rawScoreService.uploadGradescopeCSV(file.getInputStream(), testId).getFirst();

        fileContent = "First Name,Last Name,SID,,,,Total Score,Max Points,Status,,Submission Time,Lateness (H:M:S)\n" +
                "Jane,Doe,12344321,,,,12.0,12.0,Graded,,2022-07-25 23:20:26 -0600,15:29:30\n";

        file = new MockMultipartFile(filename, filename, "text/csv", fileContent.getBytes());
        testId = UUID.randomUUID();

        RawScore secondRawScore = rawScoreService.uploadGradescopeCSV(file.getInputStream(), testId).getFirst();

        Assertions.assertNotEquals(firstRawScore.getScore(), secondRawScore.getScore());
        Assertions.assertNotEquals(firstRawScore.getSubmissionTime(), secondRawScore.getSubmissionTime());
        Assertions.assertNotEquals(firstRawScore.getHoursLate(), secondRawScore.getHoursLate(), 0.0001);

    }

    @Test
    @SneakyThrows
    void testAllGraded(){
        String fileContent = "First Name,Last Name,SID,Email,Sections,section_name,Total Score,Max Points,Status,Submission ID,Submission Time,Lateness (H:M:S)\n" +
                "Samual,Mcsam,101,samualmcsam@mines.edu,,,8.0,12.0,Graded,128746829,2022-06-25 13:16:26 -0600,00:00:00\n" +
                "Robert,Bob,robbob,robbob@mines.edu,,,6.0,12.0,Graded,128746844,2022-06-25 13:16:58 -0600,00:00:00\n" +
                "Null,IdontNull,abcdefg,nullnullnull@mymail.mines.edu,,,12.0,12.0,Graded,128746851,2022-06-25 13:17:12 -0600,02:00:00";

        String filename = "test.csv";
        MockMultipartFile file = new MockMultipartFile(filename, filename, "text/csv", fileContent.getBytes());
        UUID testId = UUID.randomUUID();

        List<RawScore> rawScoreList = rawScoreService.uploadGradescopeCSV(file.getInputStream(), testId);

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
        String fileContent = "First Name,Last Name,SID,Email,Sections,section_name,Total Score,Max Points,Status,Submission ID,Submission Time,Lateness (H:M:S)\n" +
                "Samual,Mcsam,101,samualmcsam@mines.edu,,,6.0,12.0,Ungraded,128746829,2022-06-25 13:16:26 -0600,00:00:00\n" +
                "Robert,Bob,robbob,robbob@mines.edu,,,4.0,12.0,Ungraded,128746844,2022-06-25 13:16:58 -0600,00:00:00\n" +
                "Null,IdontNull,abcdefg,nullnullnull@mymail.mines.edu,,,0.0,12.0,Ungraded,128746851,2022-06-25 13:17:12 -0600,02:00:00";

        String filename = "test.csv";
        MockMultipartFile file = new MockMultipartFile(filename, filename, "text/csv", fileContent.getBytes());
        UUID testId = UUID.randomUUID();

        List<RawScore> rawScoreList = rawScoreService.uploadGradescopeCSV(file.getInputStream(), testId);

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
        String fileContent = "First Name,Last Name,SID,Email,Sections,section_name,Total Score,Max Points,Status,Submission ID,Submission Time,Lateness (H:M:S)\n" +
                "Samual,Mcsam,101,samualmcsam@mines.edu,,,,12.0,Missing,128746829,,\n" +
                "Robert,Bob,robbob,robbob@mines.edu,,,,12.0,Missing,128746844,,\n" +
                "Null,IdontNull,abcdefg,nullnullnull@mymail.mines.edu,,,,12.0,Missing,128746851,,";

        String filename = "test.csv";
        MockMultipartFile file = new MockMultipartFile(filename, filename, "text/csv", fileContent.getBytes());
        UUID testId = UUID.randomUUID();

        List<RawScore> rawScoreList = rawScoreService.uploadGradescopeCSV(file.getInputStream(), testId);

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

    @Test
    @SneakyThrows
    void testParseRunestone(){
        String fileContent = "\"first_name\",\"last_name\",\"email\",\"Week 6 Readings\",\"Week 12 Readings\",\"Week 11 Readings\",\"Week 14 Readings\",\"Week 10 Readings\",\"Week 4 Readings\",\"Week 1 Readings\",\"Week 7 Readings\",\"Week 2 Readings\",\"Week 13 Readings\",\"Week 3 Readings\",\"Week 5 Readings\",\"Week 8 Readings\"\n" +
                "\"Test\",\"User\",\"test_user@mines.edu\",\"0.0\",\"\",\"\",\"\",\"4.0\",\"2.0\",\"3.0\",\"16.0\",\"23.0\",\"1.0\",\"13.0\",\"1.0\",\"0.0\"\n" +
                "\"Alex\",\"User\",\"alex_user@mines.edu\",\"0.0\",\"\",\"\",\"\",\"\",\"2.0\",\"24.0\",\"16.0\",\"23.0\",\"\",\"13.0\",\"5.0\",\"0.0\"\n";

        String filename = "test.csv";
        MockMultipartFile file = new MockMultipartFile(filename, filename, "text/csv", fileContent.getBytes());
        UUID testId = UUID.randomUUID();

        // Should add scores to the list that were properly saved
        List<RawScore> scores = rawScoreService.uploadRunestoneCSV(file.getInputStream(), testId);

        Assertions.assertFalse(scores.isEmpty());

        // TODO finish this test
    }
}