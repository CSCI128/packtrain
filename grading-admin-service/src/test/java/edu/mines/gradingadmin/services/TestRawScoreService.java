package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.containers.PostgresTestContainer;
import edu.mines.gradingadmin.models.RawScore;
import edu.mines.gradingadmin.models.SubmissionStatus;
import edu.mines.gradingadmin.repositories.RawScoreRepo;
import jakarta.transaction.Transactional;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

    @Test
    @SneakyThrows
    void testParse(){

        String fileContent = "First Name,Last Name,SID,,,,Total Score,Max Points,Status,,Submission Time,Lateness (H:M:S),,,,,,,,,,,,,,,,,,\n" +
                "Jane,Doe,12344321,,,,12.0,12.0,Graded,,2022-06-25 13:16:26 -0600,13:29:30,,,,,,,,,,,,,,,,,,\n" +
                "Tester,Testing,testtest,,,,12.0,12.0,Graded,,2022-06-25 13:16:58 -0600,00:00:00,,,,,,,,,,,,,,,,,,\n" +
                "Jimmy,yyy,jimmyyyy,,,,12.0,12.0,Graded,,2022-06-25 13:17:12 -0600,00:00:00,,,,,,,,,,,,,,,,,,\n";

        String filename = "test.csv";
        MockMultipartFile file = new MockMultipartFile(filename, filename, "text/csv", fileContent.getBytes());
        UUID testId = UUID.randomUUID();

        // TODO: Split these up into separate tests

        // Should add scores to the list that were properly saved
        List<RawScore> scores = rawScoreService.uploadCSV(file, testId);
        Assertions.assertFalse(scores.isEmpty());

        // Test to skip the first line of the csv
        Assertions.assertEquals(Optional.empty(), rawScoreService.getRawScoreFromCwidAndAssignmentId("SID", testId));

        // Singular test get on saved score
        Optional<RawScore> score = rawScoreService.getRawScoreFromCwidAndAssignmentId("12344321", testId);
        Assertions.assertFalse(score.isEmpty());
        Assertions.assertEquals(SubmissionStatus.GRADED, score.get().getSubmissionStatus());

        // TODO: test other fields that convert weird, like Lateness/Submission Time
    }

    // TODO: Test overwriting raw scores

    // TODO: Add a test for all Graded, all Ungraded, all Missing.

}
