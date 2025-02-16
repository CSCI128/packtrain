package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.containers.PostgresTestContainer;
import edu.mines.gradingadmin.models.Assignment;
import edu.mines.gradingadmin.models.Course;
import edu.mines.gradingadmin.repositories.UserRepo;
import edu.mines.gradingadmin.seeders.CourseSeeders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class TestAssignmentService implements PostgresTestContainer {
    @Autowired
    private CourseSeeders courseSeeders;

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private UserRepo userRepo;

    @BeforeAll
    static void setupClass(){
        postgres.start();
    }

    @AfterEach
    void tearDown(){
        courseSeeders.clearAll();
        userRepo.deleteAll();
    }

    @Test
    void verifyGetAssignmentUnlocked(){
        Course course = courseSeeders.populatedCourse();

        List<Assignment> assignments = assignmentService.getAllUnlockedAssignments(course.getId().toString());

        Assertions.assertEquals(course.getAssignments().size(), assignments.size());
    }

}
