package edu.mines.packtrain.services;

import edu.mines.packtrain.containers.PostgresTestContainer;
import edu.mines.packtrain.data.AssignmentDTO;
import edu.mines.packtrain.factories.DTOFactory;
import edu.mines.packtrain.models.Assignment;
import edu.mines.packtrain.models.Course;
import edu.mines.packtrain.repositories.UserRepo;
import edu.mines.packtrain.seeders.CourseSeeders;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.List;

@SpringBootTest
@Transactional
public class TestAssignmentService implements PostgresTestContainer {

    @Autowired
    private AssignmentService assignmentService;
    @Autowired
    private CourseSeeders courseSeeders;
    @Autowired
    private UserRepo userRepo;

    @BeforeAll
    static void setupClass(){
        postgres.start();
    }

    @BeforeEach
    void setup() {

    }

    @AfterEach
    void tearDown(){
        courseSeeders.clearAll();
        userRepo.deleteAll();
    }

    @Test
    void verifyAddAssignments() {
        Course course = courseSeeders.course1();

        Assignment assignment = assignmentService.addAssignmentToCourse(
                course,
                new AssignmentDTO().name("Studio 1").points(2.0).category("Studios").dueDate(Instant.now()).unlockDate(Instant.now()).enabled(true)
        );

        Assertions.assertTrue(assignment.isEnabled());
    }

    @Test
    void verifyGetAssignmentUnlocked(){
        Course course = courseSeeders.populatedCourse();

        List<Assignment> assignments = assignmentService.getAllUnlockedAssignments(course);

        Assertions.assertEquals(course.getAssignments().size(), assignments.size());
    }

    @Test
    void verifyUpdateAssignment() {
        Course course = courseSeeders.course1();

        Assignment assignment = assignmentService.addAssignmentToCourse(
                course,
                new AssignmentDTO().name("Studio 1").points(2.0).category("Studios").dueDate(Instant.now()).unlockDate(Instant.now()).enabled(true)
        );

        assignment = assignmentService.updateAssignment(course, DTOFactory.toDto(assignment).points(10.0));

        Assertions.assertEquals(10, assignment.getPoints());
    }
}
