package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.containers.PostgresTestContainer;
import edu.mines.gradingadmin.models.*;
import edu.mines.gradingadmin.repositories.ExtensionRepo;
import edu.mines.gradingadmin.seeders.CourseSeeders;
import edu.mines.gradingadmin.seeders.UserSeeders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Set;

@SpringBootTest
public class TestExtensionService implements PostgresTestContainer {

    @Autowired ExtensionService extensionService;

    @Autowired
    private ExtensionRepo extensionRepo;
    @Autowired
    private CourseSeeders courseSeeders;

    @BeforeAll
    static void setupClass(){
        postgres.start();
    }

    @AfterEach
    void tearDown(){
        extensionRepo.deleteAll();
    }

    // TO-DO: test that getAllExtensions does what is expected
    // Will need to create a migration seed in order to write this test
    // This test should be implemented when the ExtensionService is implemented
    @Test
    void verifyGetAllExtensions(){


    }

}
