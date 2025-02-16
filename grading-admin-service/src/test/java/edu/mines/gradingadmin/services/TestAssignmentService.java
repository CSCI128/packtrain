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

        List<AssignmentDTO> assignmentDTOs = new ArrayList<>();
        assignmentDTOs.add(new AssignmentDTO()
                .category("Studios")
                .dueDate(Instant.now())
                .unlockDate(Instant.now())
                .enabled(true)
                .points(2.0));
        assignmentDTOs.add(new AssignmentDTO()
                .category("Assessments")
                .dueDate(Instant.now())
                .unlockDate(Instant.now())
                .enabled(false)
                .points(5.0));

        assignmentService.addAssignmentToCourse(course1.getId().toString(), assignmentDTOs);

        Optional<Course> course = courseService.getCourse(course1.getId());

        Assertions.assertTrue(course.isPresent());
        Assertions.assertEquals(2, course.get().getAssignments().size());
    }

    @Test
    void verifyGetAssignmentUnlocked(){
        Course course = courseSeeders.populatedCourse();

        List<Assignment> assignments = assignmentService.getAllUnlockedAssignments(course.getId().toString());

        Assertions.assertEquals(course.getAssignments().size(), assignments.size());
    }

}
