package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.containers.PostgresTestContainer;
import edu.mines.gradingadmin.managers.IdentityProvider;
import edu.mines.gradingadmin.managers.ImpersonationManager;
import edu.mines.gradingadmin.managers.SecurityManager;
import edu.mines.gradingadmin.models.Course;
import edu.mines.gradingadmin.models.User;
import edu.mines.gradingadmin.models.tasks.CourseImportTaskDef;
import edu.mines.gradingadmin.repositories.*;
import edu.mines.gradingadmin.seeders.CanvasSeeder;
import edu.mines.gradingadmin.seeders.CanvasSeeder;
import edu.mines.gradingadmin.seeders.CourseSeeders;
import edu.mines.gradingadmin.seeders.UserSeeders;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    @Autowired
    private UserSeeders userSeeders;
    @Autowired
    private ImpersonationManager impersonationManager;

    @Autowired
    private ScheduledTaskRepo<CourseImportTaskDef> scheduledTaskRepo;


    @BeforeAll
    static void setupClass() {
        postgres.start();

    }

    @BeforeEach
    void setup(){
        courseService = new CourseService(
                courseRepo, scheduledTaskRepo,
                Mockito.mock(ApplicationEventPublisher.class),
                impersonationManager, canvasService
        );

        applyMocks(canvasService);
    }


    @AfterEach
    void tearDown() {
        userSeeders.clearAll();
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
        Course course = courseSeeders.course1();
        User admin = userSeeders.admin1();

        CourseImportTaskDef taskDef = new CourseImportTaskDef();
        taskDef.setCreatedByUser(admin);
        taskDef.setCourseToImport(course.getId());
        taskDef.setCanvasId(course1Id);
        taskDef.setOverwriteCode(true);
        taskDef.setOverwriteName(true);

        courseService.syncCourseTask(taskDef);

        course = courseService.getCourse(course.getId()).orElseThrow(AssertionError::new);

        Assertions.assertEquals(course1.get().getCourseCode(), course.getCode());
        Assertions.assertEquals(course1.get().getName(), course.getName());
    }

    @Test
    void verifyCourseDoesNotExist(){
        Optional<Course> course = courseService.createNewCourse("Test Course 1", "Fall 2024", "fall.2024.tc.1");
        Assertions.assertTrue(course.isPresent());
        Assertions.assertNotNull(course.get().getId());
    }

    @Test
    void verifyNewCourseHasName(){
        Optional<Course> course = courseService.createNewCourse("Another Test Course 1", "Spring 2025", "spring.2025.atc.1");
        Assertions.assertTrue(course.isPresent());
        Assertions.assertEquals("Another Test Course 1", course.get().getName());
    }

    @Test
    void verifyNewCourseHasTerm(){
        Optional<Course> course = courseService.createNewCourse("Another Test Course 1", "Spring 2025", "spring.2025.atc.1");
        Assertions.assertTrue(course.isPresent());
        Assertions.assertEquals("Spring 2025", course.get().getTerm());
    }

    @Test
    void verifyNewCourseHasCode(){
        Optional<Course> course = courseService.createNewCourse("Test Course 1", "Fall 2024", "fall.2024.tc.1");
        Assertions.assertTrue(course.isPresent());
        Assertions.assertEquals("fall.2024.tc.1", course.get().getCode());
    }

}
