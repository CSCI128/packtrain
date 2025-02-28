package edu.mines.gradingadmin.services;


import edu.mines.gradingadmin.models.Course;
import edu.mines.gradingadmin.models.MasterMigration;
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
import java.util.Optional;

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
    void verifyGetMigrations() {
        Course course1 = courseSeeders.course1();
        List<Migration> migrations = migrationService.getAllMigrations(course1.getId().toString());

        // how do I create a migration seeder in order to test this?
    }


}
