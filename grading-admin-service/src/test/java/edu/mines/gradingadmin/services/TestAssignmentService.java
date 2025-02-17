package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.containers.PostgresTestContainer;
import edu.mines.gradingadmin.data.AssignmentDTO;
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
        Course course1 = courseSeeders.course1();

        AssignmentDTO assignmentDTO = new AssignmentDTO()
                .id(UUID.randomUUID().toString())
                .name("Studio 1")
                .category("Studios")
                .dueDate(Instant.now())
                .unlockDate(Instant.now())
                .enabled(true)
                .points(2.0);

        assignmentService.addAssignmentToCourse(course1.getId().toString(),
                assignmentDTO.getName(),
                assignmentDTO.getPoints(),
                assignmentDTO.getCategory(),
                assignmentDTO.getEnabled(),
                assignmentDTO.getDueDate(),
                assignmentDTO.getUnlockDate());

        Optional<Course> course = courseService.getCourse(course1.getId());

        Assertions.assertTrue(course.isPresent());
        Assertions.assertEquals(1, course.get().getAssignments().size());
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

        AssignmentDTO assignmentDTO = new AssignmentDTO()
                .id(UUID.randomUUID().toString())
                .name("Studio 1")
                .category("Studios")
                .dueDate(Instant.now())
                .unlockDate(Instant.now())
                .enabled(true)
                .points(5.0);

        Optional<Assignment> assignment = assignmentService.addAssignmentToCourse(course.getId().toString(),
                assignmentDTO.getName(),
                assignmentDTO.getPoints(),
                assignmentDTO.getCategory(),
                assignmentDTO.getEnabled(),
                assignmentDTO.getDueDate(),
                assignmentDTO.getUnlockDate());

        Assertions.assertTrue(assignment.isPresent());

        assignment = assignmentService.updateAssignment(course.getId().toString(),
                assignment.get().getId().toString(),
                assignment.get().getName(),
                10,
                assignment.get().getCategory(),
                assignment.get().isEnabled(),
                assignment.get().getDueDate(),
                assignment.get().getUnlockDate());

        course = courseService.getCourse(course.getId()).orElseThrow(AssertionError::new);

        Assertions.assertEquals(1, course.getAssignments().size());

        Assertions.assertTrue(assignment.isPresent());
        Assertions.assertEquals(10, assignment.get().getPoints());
    }
}
