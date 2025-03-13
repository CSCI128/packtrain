package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.containers.PostgresTestContainer;
import edu.mines.gradingadmin.models.RawScore;
import edu.mines.gradingadmin.models.SubmissionStatus;
import edu.mines.gradingadmin.repositories.RawScoreRepo;
import jakarta.transaction.Transactional;
import lombok.SneakyThrows;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

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

        List<RawScore> rawScores = rawScoreService.uploadCSV(file, testId);

        Assertions.assertTrue(rawScores.isEmpty());
    }

    @Test
    @SneakyThrows
    void testSkipFirst(){
        String fileContent = "First Name,Last Name,SID,,,,Total Score,Max Points,Status,,Submission Time,Lateness (H:M:S),,,,,,,,,,,,,,,,,,\n";

        String filename = "test.csv";
        MockMultipartFile file = new MockMultipartFile(filename, filename, "text/csv", fileContent.getBytes());
        UUID testId = UUID.randomUUID();

        List<RawScore> rawScores = rawScoreService.uploadCSV(file, testId);

        Assertions.assertTrue(rawScores.isEmpty());
    }

    @Test
    @SneakyThrows
    void testParse(){

        String fileContent = "First Name,Last Name,SID,,,,Total Score,Max Points,Status,,Submission Time,Lateness (H:M:S),,,,,,,,,,,,,,,,,,\n" +
                "Jane,Doe,12344321,,,,12.0,12.0,Graded,,2022-06-25 13:16:26 -0600,13:29:30,,,,,,,,,,,,,,,,,,\n" +
                "Tester,Testing,testtest,,,,12.0,12.0,Graded,,2022-06-25 13:16:58 -0600,00:00:00,,,,,,,,,,,,,,,,,,\n" +
                "Jimmy,yyy,jimmyyyy,,,,11.5,12.0,Graded,,2022-06-25 13:25:12 -0600,07:32:50,,,,,,,,,,,,,,,,,,\n" +
                "Joe,Jam,121212,,,,,12.0,Missing,,,,,,,,,,,,,,,,,,,,,\n";

        String filename = "test.csv";
        MockMultipartFile file = new MockMultipartFile(filename, filename, "text/csv", fileContent.getBytes());
        UUID testId = UUID.randomUUID();

        // Should add scores to the list that were properly saved
        List<RawScore> scores = rawScoreService.uploadCSV(file, testId);
        Assertions.assertFalse(scores.isEmpty());

        // Singular test get on saved score
        Optional<RawScore> score = rawScoreService.getRawScoreFromCwidAndAssignmentId("12344321", testId);
        Assertions.assertFalse(score.isEmpty());
        Assertions.assertEquals(SubmissionStatus.GRADED, score.get().getSubmissionStatus());

        // Test weird conversion fields
        score = rawScoreService.getRawScoreFromCwidAndAssignmentId("121212", testId);
        Assertions.assertNull(score.get().getScore());
        Assertions.assertEquals(SubmissionStatus.MISSING, score.get().getSubmissionStatus());
        Assertions.assertNull(score.get().getHoursLate());
        Assertions.assertNull(score.get().getSubmissionTime());

        // Test weird conversion fields
        score = rawScoreService.getRawScoreFromCwidAndAssignmentId("jimmyyyy", testId);
        Assertions.assertEquals(11.5, score.get().getScore());
        Assertions.assertEquals(7.5472, score.get().getHoursLate(), 0.001);
        Assertions.assertEquals("2022-06-25T19:25:12Z", score.get().getSubmissionTime().toString());

    }

    @Test
    @SneakyThrows
    void testOverwrite(){
        String fileContent = "First Name,Last Name,SID,,,,Total Score,Max Points,Status,,Submission Time,Lateness (H:M:S),,,,,,,,,,,,,,,,,,\n" +
                "Jane,Doe,12344321,,,,0.0,12.0,Graded,,2022-06-25 13:16:26 -0600,13:29:30,,,,,,,,,,,,,,,,,,\n";

        String filename = "test.csv";
        MockMultipartFile file = new MockMultipartFile(filename, filename, "text/csv", fileContent.getBytes());
        UUID testId = UUID.randomUUID();

        RawScore firstRawScore = rawScoreService.uploadCSV(file, testId).getFirst();

        fileContent = "First Name,Last Name,SID,,,,Total Score,Max Points,Status,,Submission Time,Lateness (H:M:S),,,,,,,,,,,,,,,,,,\n" +
                "Jane,Doe,12344321,,,,12.0,12.0,Graded,,2022-07-25 23:20:26 -0600,15:29:30,,,,,,,,,,,,,,,,,,\n";

        file = new MockMultipartFile(filename, filename, "text/csv", fileContent.getBytes());
        testId = UUID.randomUUID();

        RawScore secondRawScore = rawScoreService.uploadCSV(file, testId).getFirst();

        Assertions.assertNotEquals(firstRawScore.getScore(), secondRawScore.getScore());
        Assertions.assertNotEquals(firstRawScore.getSubmissionTime(), secondRawScore.getSubmissionTime());
        Assertions.assertNotEquals(firstRawScore.getHoursLate(), secondRawScore.getHoursLate(), 0.0001);

    }
    // TODO: Add a test for all Graded, all Ungraded, all Missing.

    @Test
    @SneakyThrows
    void testAllGraded(){
        String fileContent = "First Name,Last Name,SID,Email,Sections,section_name,Total Score,Max Points,Status,Submission ID,Submission Time,Lateness (H:M:S),View Count,Submission Count,1.1: A (0.25 pts),1.2: B (0.25 pts),1.3: C (0.25 pts),1.4: D (0.25 pts),2.1: A (0.5 pts),2.2: B (0.5 pts),2.3: C (0.5 pts),2.4: D (0.5 pts),3: Drawing Truth Table from Expression (2.0 pts),4.1: A (0.5 pts),4.2: B (0.5 pts),4.3: C (0.5 pts),4.4: D (0.5 pts),5: Output from Circuit Diagram (1.0 pts),6: Circuit Diagram from Boolean Expression (2.0 pts),7: Boolean Expression and Circuit Diagram from Table (2.0 pts)\n" +
                "Elena,Ramirez,10866111,emramirez@mines.edu,,,8.0,12.0,Graded,128746829,2022-06-25 13:16:26 -0600,00:00:00,0,1,0.25,0.25,0.25,0.25,0.5,0.5,0.5,0.5,2.0,0.5,0.5,0.5,0.5,1.0,2.0,2.0\n" +
                "Megan,Shapiro,mshapiro,mshapiro@mines.edu,,,6.0,12.0,Graded,128746844,2022-06-25 13:16:58 -0600,00:00:00,0,1,0.25,0.25,0.25,0.25,0.5,0.5,0.5,0.5,2.0,0.5,0.5,0.5,0.5,1.0,2.0,2.0\n" +
                "Robert,Christian,10868072,rchristian@mymail.mines.edu,,,12.0,12.0,Graded,128746851,2022-06-25 13:17:12 -0600,02:00:00,0,1,0.25,0.25,0.25,0.25,0.5,0.5,0.5,0.5,2.0,0.5,0.5,0.5,0.5,1.0,2.0,2.0";

        String filename = "test.csv";
        MockMultipartFile file = new MockMultipartFile(filename, filename, "text/csv", fileContent.getBytes());
        UUID testId = UUID.randomUUID();

        List<RawScore> rawScoreList = rawScoreService.uploadCSV(file, testId);

        Assertions.assertEquals("10866111", rawScoreList.get(0).getCwid());
        Assertions.assertEquals("mshapiro", rawScoreList.get(1).getCwid());
        Assertions.assertEquals("10868072", rawScoreList.get(2).getCwid());

        Assertions.assertEquals("2022-06-25T19:16:26Z", rawScoreList.get(0).getSubmissionTime().toString());
        Assertions.assertEquals("2022-06-25T19:16:58Z", rawScoreList.get(1).getSubmissionTime().toString());
        Assertions.assertEquals("2022-06-25T19:17:12Z", rawScoreList.get(2).getSubmissionTime().toString());

        Assertions.assertNotNull(rawScoreList.get(0));
        Assertions.assertNotNull(rawScoreList.get(1));
        Assertions.assertNotNull(rawScoreList.get(2));

        Assertions.assertEquals(8.0, rawScoreList.get(0).getScore());
        Assertions.assertEquals(6.0, rawScoreList.get(1).getScore());
        Assertions.assertEquals(12.0, rawScoreList.get(2).getScore());

        Assertions.assertEquals(SubmissionStatus.GRADED, rawScoreList.get(0).getSubmissionStatus());
        Assertions.assertEquals(SubmissionStatus.GRADED, rawScoreList.get(1).getSubmissionStatus());
        Assertions.assertEquals(SubmissionStatus.GRADED, rawScoreList.get(2).getSubmissionStatus());

        Assertions.assertEquals(0, rawScoreList.get(0).getHoursLate());
        Assertions.assertEquals(0, rawScoreList.get(1).getHoursLate());
        Assertions.assertEquals(2, rawScoreList.get(2).getHoursLate());

    }

    @Test
    @SneakyThrows
    void testAllUngraded(){
        String fileContent = "First Name,Last Name,SID,Email,Sections,section_name,Total Score,Max Points,Status,Submission ID,Submission Time,Lateness (H:M:S),View Count,Submission Count,1.1: A (0.25 pts),1.2: B (0.25 pts),1.3: C (0.25 pts),1.4: D (0.25 pts),2.1: A (0.5 pts),2.2: B (0.5 pts),2.3: C (0.5 pts),2.4: D (0.5 pts),3: Drawing Truth Table from Expression (2.0 pts),4.1: A (0.5 pts),4.2: B (0.5 pts),4.3: C (0.5 pts),4.4: D (0.5 pts),5: Output from Circuit Diagram (1.0 pts),6: Circuit Diagram from Boolean Expression (2.0 pts),7: Boolean Expression and Circuit Diagram from Table (2.0 pts)\n" +
                "Elena,Ramirez,10866111,emramirez@mines.edu,,,8.0,12.0,Ungraded,128746829,2022-06-25 13:16:26 -0600,00:00:00,0,1,0.25,0.25,0.25,0.25,0.5,0.5,0.5,0.5,2.0,0.5,0.5,0.5,0.5,1.0,2.0,2.0\n" +
                "Megan,Shapiro,mshapiro,mshapiro@mines.edu,,,6.0,12.0,Ungraded,128746844,2022-06-25 13:16:58 -0600,00:00:00,0,1,0.25,0.25,0.25,0.25,0.5,0.5,0.5,0.5,2.0,0.5,0.5,0.5,0.5,1.0,2.0,2.0\n" +
                "Robert,Christian,10868072,rchristian@mymail.mines.edu,,,12.0,12.0,Ungraded,128746851,2022-06-25 13:17:12 -0600,02:00:00,0,1,0.25,0.25,0.25,0.25,0.5,0.5,0.5,0.5,2.0,0.5,0.5,0.5,0.5,1.0,2.0,2.0";

        String filename = "test.csv";
        MockMultipartFile file = new MockMultipartFile(filename, filename, "text/csv", fileContent.getBytes());
        UUID testId = UUID.randomUUID();

        List<RawScore> rawScoreList = rawScoreService.uploadCSV(file, testId);

        Assertions.assertEquals("10866111", rawScoreList.get(0).getCwid());
        Assertions.assertEquals("mshapiro", rawScoreList.get(1).getCwid());
        Assertions.assertEquals("10868072", rawScoreList.get(2).getCwid());

        Assertions.assertEquals("2022-06-25T19:16:26Z", rawScoreList.get(0).getSubmissionTime().toString());
        Assertions.assertEquals("2022-06-25T19:16:58Z", rawScoreList.get(1).getSubmissionTime().toString());
        Assertions.assertEquals("2022-06-25T19:17:12Z", rawScoreList.get(2).getSubmissionTime().toString());

        Assertions.assertNotNull(rawScoreList.get(0));
        Assertions.assertNotNull(rawScoreList.get(1));
        Assertions.assertNotNull(rawScoreList.get(2));

        Assertions.assertEquals(8.0, rawScoreList.get(0).getScore());
        Assertions.assertEquals(6.0, rawScoreList.get(1).getScore());
        Assertions.assertEquals(12.0, rawScoreList.get(2).getScore());

        Assertions.assertEquals(SubmissionStatus.UNGRADED, rawScoreList.get(0).getSubmissionStatus());
        Assertions.assertEquals(SubmissionStatus.UNGRADED, rawScoreList.get(1).getSubmissionStatus());
        Assertions.assertEquals(SubmissionStatus.UNGRADED, rawScoreList.get(2).getSubmissionStatus());

        Assertions.assertEquals(0, rawScoreList.get(0).getHoursLate());
        Assertions.assertEquals(0, rawScoreList.get(1).getHoursLate());
        Assertions.assertEquals(2, rawScoreList.get(2).getHoursLate());

    }

    @Test
    @SneakyThrows
    void testAllMissing() {
        String fileContent = "First Name,Last Name,SID,Email,Sections,section_name,Total Score,Max Points,Status,Submission ID,Submission Time,Lateness (H:M:S),View Count,Submission Count,1.1: A (0.25 pts),1.2: B (0.25 pts),1.3: C (0.25 pts),1.4: D (0.25 pts),2.1: A (0.5 pts),2.2: B (0.5 pts),2.3: C (0.5 pts),2.4: D (0.5 pts),3: Drawing Truth Table from Expression (2.0 pts),4.1: A (0.5 pts),4.2: B (0.5 pts),4.3: C (0.5 pts),4.4: D (0.5 pts),5: Output from Circuit Diagram (1.0 pts),6: Circuit Diagram from Boolean Expression (2.0 pts),7: Boolean Expression and Circuit Diagram from Table (2.0 pts)\n" +
                "Elena,Ramirez,10866111,emramirez@mines.edu,,,,12.0,Missing,128746829,,,0,1,0.25,0.25,0.25,0.25,0.5,0.5,0.5,0.5,2.0,0.5,0.5,0.5,0.5,1.0,2.0,2.0\n" +
                "Megan,Shapiro,mshapiro,mshapiro@mines.edu,,,,12.0,Missing,128746844,,,0,1,0.25,0.25,0.25,0.25,0.5,0.5,0.5,0.5,2.0,0.5,0.5,0.5,0.5,1.0,2.0,2.0\n" +
                "Robert,Christian,10868072,rchristian@mymail.mines.edu,,,,12.0,Missing,128746851,,,0,1,0.25,0.25,0.25,0.25,0.5,0.5,0.5,0.5,2.0,0.5,0.5,0.5,0.5,1.0,2.0,2.0";

        String filename = "test.csv";
        MockMultipartFile file = new MockMultipartFile(filename, filename, "text/csv", fileContent.getBytes());
        UUID testId = UUID.randomUUID();

        List<RawScore> rawScoreList = rawScoreService.uploadCSV(file, testId);

        Assertions.assertEquals("10866111", rawScoreList.get(0).getCwid());
        Assertions.assertEquals("mshapiro", rawScoreList.get(1).getCwid());
        Assertions.assertEquals("10868072", rawScoreList.get(2).getCwid());

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