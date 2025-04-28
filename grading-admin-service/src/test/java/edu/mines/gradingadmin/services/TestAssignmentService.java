package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.containers.PostgresTestContainer;
import edu.mines.gradingadmin.data.AssignmentDTO;
import edu.mines.gradingadmin.factories.DTOFactory;
import edu.mines.gradingadmin.models.Assignment;
import edu.mines.gradingadmin.models.Course;
import edu.mines.gradingadmin.repositories.UserRepo;
import edu.mines.gradingadmin.seeders.CourseSeeders;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@SpringBootTest
@Transactional
public class TestAssignmentService implements PostgresTestContainer {

    @Autowired
    private AssignmentService assignmentService;
    @Autowired
    private CourseService courseService;
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
                course.getId().toString(),
                new AssignmentDTO().name("Studio 1").points(2.0).category("Studios").dueDate(Instant.now()).unlockDate(Instant.now()).enabled(true)
        );

        Assertions.assertTrue(assignment.isEnabled());
    }

    @Test
    void verifyGetAssignmentUnlocked(){
        Course course = courseSeeders.populatedCourse();

        List<Assignment> assignments = assignmentService.getAllUnlockedAssignments(course.getId().toString());

        Assertions.assertEquals(course.getAssignments().size(), assignments.size());
    }

    @Test
    void verifyUpdateAssignment() {
        Course course = courseSeeders.course1();

        Assignment assignment = assignmentService.addAssignmentToCourse(
                course.getId().toString(),
                new AssignmentDTO().name("Studio 1").points(2.0).category("Studios").dueDate(Instant.now()).unlockDate(Instant.now()).enabled(true)
        );

        assignment = assignmentService.updateAssignment(course.getId().toString(), DTOFactory.toDto(assignment).points(10.0));

        Assertions.assertEquals(10, assignment.getPoints());
    }
}
