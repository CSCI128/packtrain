package edu.mines.gradingadmin.services;


import edu.mines.gradingadmin.containers.MinioTestContainer;
import edu.mines.gradingadmin.containers.PostgresTestContainer;
import edu.mines.gradingadmin.data.AssignmentDTO;
import edu.mines.gradingadmin.data.CourseDTO;
import edu.mines.gradingadmin.data.PolicyDTO;
import edu.mines.gradingadmin.models.*;
import edu.mines.gradingadmin.repositories.ExtensionRepo;
import edu.mines.gradingadmin.repositories.MasterMigrationRepo;
import edu.mines.gradingadmin.repositories.MigrationRepo;
import edu.mines.gradingadmin.repositories.PolicyRepo;
import edu.mines.gradingadmin.seeders.CourseSeeders;
import edu.mines.gradingadmin.seeders.UserSeeders;
import jakarta.transaction.Transactional;
import org.junit.experimental.theories.internal.Assignments;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
    @Autowired
    private AssignmentService assignmentService;


    @BeforeAll
    static void setupClass(){
        postgres.start();
        minio.start();

    }

    
    @AfterEach
    void tearDown(){
        migrationRepo.deleteAll();
        masterMigrationRepo.deleteAll();
        policyRepo.deleteAll();
        courseSeeders.clearAll();
        userSeeders.clearAll();
    }

    @Test
    void verifyGetMigrations() {
        Course course1 = courseSeeders.populatedCourse();
        User user = userSeeders.user1();
        String filename = "file.js";
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
    void verifyUpdatePolicy(){
        Course course1 = courseSeeders.populatedCourse();
        Optional<MasterMigration> masterMigration = migrationService.createMasterMigration(course1.getId().toString());
        Assertions.assertTrue(masterMigration.isPresent());
        Optional<Assignment> assignment = course1.getAssignments().stream().findFirst();
        Assertions.assertTrue(assignment.isPresent());
        String filename = "http://file.js";


        Policy policy = new Policy();
        policy.setAssignment(assignment.get());
        policy.setPolicyName("test_policy");
        policy.setPolicyURI(filename);
        policy.setCourse(course1);
        User user = userSeeders.user1();
        policy.setCreatedByUser(user);
        policyRepo.save(policy);
        migrationService.addMigration(masterMigration.get().getId().toString(), assignment.get().getId().toString(), policy.getPolicyURI());

        List<Migration> migrationList = migrationService.getMigrationsByMasterMigration(masterMigration.get().getId().toString());
        Assertions.assertEquals(1, migrationList.size());
        Assertions.assertEquals(assignment.get().getId().toString(), migrationList.getFirst().getAssignment().getId().toString());
        Assertions.assertEquals(policy.getPolicyURI(), migrationList.getFirst().getPolicy().getPolicyURI());
    }

    @Test
    void verifyMigrationsCreated() throws URISyntaxException {
        List<Migration> migrationList = migrationService.getMigrationsByMasterMigration(masterMigration.get().getId().toString());
        Migration migration = migrationList.getFirst();
        Assertions.assertEquals(1, migrationList.size());
        Assertions.assertEquals(policy.getPolicyURI(), migrationList.getFirst().getPolicy().getPolicyURI());
        Policy updatedPolicy = new Policy();
        updatedPolicy.setAssignment(assignment.get());
        updatedPolicy.setPolicyName("updated_test_policy");
        updatedPolicy.setPolicyURI(filename);
        updatedPolicy.setCourse(course1);
        updatedPolicy.setCreatedByUser(user);
        policyRepo.save(updatedPolicy);
        migrationService.updatePolicyForMigration(migration.getId().toString(), updatedPolicy);
        Assertions.assertEquals(updatedPolicy.getPolicyURI(), migrationList.getFirst().getPolicy().getPolicyURI());

    }

}
