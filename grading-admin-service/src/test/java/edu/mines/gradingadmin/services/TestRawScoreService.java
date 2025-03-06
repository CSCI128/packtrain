package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.containers.PostgresTestContainer;
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

        String fileContent = "content";

        String filename = "test.csv";
        MockMultipartFile file = new MockMultipartFile(filename, fileContent.getBytes());

        rawScoreService.uploadRawScores(file);

    }

}
