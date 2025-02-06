package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.containers.PostgresTestContainer;
import edu.mines.gradingadmin.managers.SecurityManager;
import edu.mines.gradingadmin.models.Course;
import edu.mines.gradingadmin.repositories.CourseMemberRepo;
import edu.mines.gradingadmin.repositories.CourseRepo;
import edu.mines.gradingadmin.repositories.SectionRepo;
import edu.mines.gradingadmin.seeders.CanvasSeeder;
import edu.mines.gradingadmin.seeders.CourseSeeders;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

@SpringBootTest
@Transactional
public class TestCourseService implements PostgresTestContainer, CanvasSeeder {

    @Autowired
    private CourseSeeders courseSeeders;

    @Autowired
    private CourseRepo courseRepo;

    private CourseService courseService;

    @Mock
    private CanvasService canvasService;

    @Mock
    private SecurityManager securityManager;

    @Autowired
    private SectionRepo sectionRepo;
    @Autowired
    private UserService userService;
    @Autowired
    private CourseMemberRepo courseMemberRepo;


    @BeforeAll
    static void setupClass() {
        postgres.start();

    }

    @BeforeEach
    void setup(){
        applyMocks(canvasService);

        courseService = new CourseService(securityManager, courseRepo, canvasService, new SectionService(sectionRepo, canvasService), userService, courseMemberRepo);

    }

    @AfterEach
    void tearDown() {
        courseRepo.deleteAll();
    }

    @Test
    void verifyGetCoursesActive() {
        Course activeCourse = courseSeeders.course1();
        Course inactiveCourse = courseSeeders.course2();

        List<Course> courses = courseService.getCourses(true);
        Assertions.assertEquals(1, courses.size());
        Assertions.assertTrue(courses.contains(activeCourse));
        Assertions.assertFalse(courses.contains(inactiveCourse));
    }

    @Test
    void verifyGetCoursesIncludingInactive() {
        Course activeCourse = courseSeeders.course1();
        Course inactiveCourse = courseSeeders.course2();

        List<Course> courses = courseService.getCourses(false);
        Assertions.assertEquals(2, courses.size());
        Assertions.assertTrue(courses.contains(activeCourse));
        Assertions.assertTrue(courses.contains(inactiveCourse));
    }

    @Test
    void verifyImportCourseFromCanvas(){
        // I am aware that this is hot garbage,
        // I need to take a step back from this and do another refactoring pass on it.
        Optional<Course> importedCourse = courseService.importCourseFromCanvas(String.valueOf(course1Id));

        Assertions.assertTrue(importedCourse.isPresent());
    }
}
