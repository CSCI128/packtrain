package edu.mines.gradingadmin.services;


import edu.mines.gradingadmin.containers.MinioTestContainer;
import edu.mines.gradingadmin.containers.PostgresTestContainer;
import edu.mines.gradingadmin.models.*;
import edu.mines.gradingadmin.repositories.ExtensionRepo;
import edu.mines.gradingadmin.repositories.MasterMigrationRepo;
import edu.mines.gradingadmin.repositories.MigrationRepo;
import edu.mines.gradingadmin.repositories.PolicyRepo;
import edu.mines.gradingadmin.seeders.CourseSeeders;
import edu.mines.gradingadmin.seeders.UserSeeders;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

import static edu.mines.gradingadmin.containers.PostgresTestContainer.postgres;

@SpringBootTest
public class TestMigrationService implements PostgresTestContainer, MinioTestContainer {
    @Autowired
    MigrationService migrationService;

    @Autowired
    private MigrationRepo migrationRepo;
    @Autowired
    private CourseSeeders courseSeeders;
    @Autowired
    private CourseService courseService;
    @Autowired
    private UserSeeders userSeeders;
    @Autowired
    private MasterMigrationRepo masterMigrationRepo;
    @Autowired
    private PolicyRepo policyRepo;

    @BeforeAll
    static void setupClass(){
        postgres.start();
        minio.start();

    }

    
    @AfterEach
    void tearDown(){
        migrationRepo.deleteAll();

        masterMigrationRepo.deleteAll();

    }

    @Test
    void verifyGetMigrations() {
        Course course1 = courseSeeders.populatedCourse();
        User user = userSeeders.user1();
        String filename = "file.js";
        String expectedContent = "console.log(\"test\")";
        MockMultipartFile file = new MockMultipartFile(filename, expectedContent.getBytes());
        Policy policy = new Policy();
        policy.setCreatedByUser(user);
        policy.setCourse(course1);
        policy.setPolicyName("test_policy");
        policy.setPolicyURI(filename);
        policyRepo.save(policy);

        MasterMigration masterMigration = migrationService.createMigrationForAssignments(course1, List.of(policy), course1.getAssignments().stream().toList());
        Assertions.assertEquals(1, masterMigrationRepo.getMasterMigrationsByCourseId(course1.getId()).size());
        Assertions.assertEquals(masterMigration, masterMigrationRepo.getMasterMigrationsByCourseId(course1.getId()).get(0));
    }

    @Test
    void verifyCreateMasterMigration(){
        Course course1 = courseSeeders.populatedCourse();

        Optional<MasterMigration> masterMigration = migrationService.createMasterMigration(course1.getId().toString());
        Assertions.assertTrue(masterMigration.isPresent());
        List<Migration> migrationList = migrationService.getMigrationsByMasterMigration(masterMigration.get().getId().toString());
        Assertions.assertEquals(0, migrationList.size());

    }

    @Test
    void verifyMigrationsCreated(){

    }


}
