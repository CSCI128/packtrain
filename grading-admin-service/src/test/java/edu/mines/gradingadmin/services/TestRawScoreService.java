package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.containers.PostgresTestContainer;
import edu.mines.gradingadmin.models.RawScore;
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
import java.util.UUID;

@SpringBootTest
@Transactional
public class TestRawScoreService implements PostgresTestContainer {

    @Autowired
    private RawScoreRepo rawScoreRepo;

    private RawScoreService rawScoreService;

    @BeforeAll
    static void setupClass(){
        postgres.start();
    }

    @BeforeEach
    void setup(){
        rawScoreService = new RawScoreService(rawScoreRepo);
    }

    @Test
    @SneakyThrows
    void testParse(){

        String fileContent = "First Name,Last Name,SID,Email,Sections,section_name,Total Score,Max Points,Status,Submission ID,Submission Time,Lateness (H:M:S),View Count,Submission Count,1.1: A (0.25 pts),1.2: B (0.25 pts),1.3: C (0.25 pts),1.4: D (0.25 pts),2.1: A (0.5 pts),2.2: B (0.5 pts),2.3: C (0.5 pts),2.4: D (0.5 pts),3: Drawing Truth Table from Expression (2.0 pts),4.1: A (0.5 pts),4.2: B (0.5 pts),4.3: C (0.5 pts),4.4: D (0.5 pts),5: Output from Circuit Diagram (1.0 pts),6: Circuit Diagram from Boolean Expression (2.0 pts),7: Boolean Expression and Circuit Diagram from Table (2.0 pts)\n" +
                "Elena,Ramirez,10866111,emramirez@mines.edu,,,12.0,12.0,Graded,128746829,2022-06-25 13:16:26 -0600,00:00:00,0,1,0.25,0.25,0.25,0.25,0.5,0.5,0.5,0.5,2.0,0.5,0.5,0.5,0.5,1.0,2.0,2.0\n" +
                "Megan,Shapiro,mshapiro,mshapiro@mines.edu,,,12.0,12.0,Graded,128746844,2022-06-25 13:16:58 -0600,00:00:00,0,1,0.25,0.25,0.25,0.25,0.5,0.5,0.5,0.5,2.0,0.5,0.5,0.5,0.5,1.0,2.0,2.0\n" +
                "Robert,Christian,10868072,rchristian@mymail.mines.edu,,,12.0,12.0,Graded,128746851,2022-06-25 13:17:12 -0600,00:00:00,0,1,0.25,0.25,0.25,0.25,0.5,0.5,0.5,0.5,2.0,0.5,0.5,0.5,0.5,1.0,2.0,2.0\n" +
                "Luca,Ciancanelli,10889236,leciancanelli@mymail.mines.edu,,,12.0,12.0,Graded,128746855,2022-06-25 13:17:22 -0600,00:00:00,0,1,0.25,0.25,0.25,0.25,0.5,0.5,0.5,0.5,2.0,0.5,0.5,0.5,0.5,1.0,2.0,2.0\n" +
                "Kai,Page,10868577,kpage1@mines.edu,,,12.0,12.0,Graded,128746857,2022-06-25 13:17:34 -0600,00:00:00,0,1,0.25,0.25,0.25,0.25,0.5,0.5,0.5,0.5,2.0,0.5,0.5,0.5,0.5,1.0,2.0,2.0\n" +
                "Megan,McFeeters,10887447,mmcfeeters@mines.edu,,,12.0,12.0,Graded,128746858,2022-06-25 13:17:45 -0600,00:00:00,0,1,0.25,0.25,0.25,0.25,0.5,0.5,0.5,0.5,2.0,0.5,0.5,0.5,0.5,1.0,2.0,2.0\n" +
                "Holly,Vose,10850093,vose@mines.edu,,,12.0,12.0,Graded,129969812,2022-07-31 10:00:35 -0600,00:00:00,0,1,0.25,0.25,0.25,0.25,0.5,0.5,0.5,0.5,2.0,0.5,0.5,0.5,0.5,1.0,2.0,2.0";

        String filename = "test.csv";
        MockMultipartFile file = new MockMultipartFile(filename, filename, "application/csv", fileContent.getBytes());

        List<RawScore> scores = rawScoreService.uploadRawScores(file, UUID.fromString("790f7d2a-97c5-4680-9e61-7681f750e320"), UUID.fromString("790f7d2a-97c5-4680-9e61-7681f750e320"));

        //Assertions.assertFalse(scores.isEmpty());

    }

}
