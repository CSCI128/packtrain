package edu.mines.gradingadmin.services;


import edu.mines.gradingadmin.models.Course;
import edu.mines.gradingadmin.models.Migration;
import edu.mines.gradingadmin.repositories.ExtensionRepo;
import edu.mines.gradingadmin.repositories.MigrationRepo;
import edu.mines.gradingadmin.seeders.CourseSeeders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static edu.mines.gradingadmin.containers.PostgresTestContainer.postgres;

@SpringBootTest
public class TestMigrationService {
    @Autowired
    MigrationService migrationService;

    @Autowired
    private MigrationRepo migrationRepo;
    @Autowired
    private CourseSeeders courseSeeders;

    @BeforeAll
    static void setupClass(){
        postgres.start();
    }

    @AfterEach
    void tearDown(){
        migrationRepo.deleteAll();
    }

    @Test
    void verifyGetAllMigration(){
       Course course = courseSeeders.populatedCourse();
       List<Migration> migrations = migrationService.getAllMigrations(course.getId().toString());
       // need to think about how to assert this in a way that makes the most sense
       Assertions.assertEquals();

    }
}
