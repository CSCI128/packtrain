package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.containers.MinioTestContainer;
import edu.mines.gradingadmin.containers.PostgresTestContainer;
import edu.mines.gradingadmin.data.CourseDTO;
import edu.mines.gradingadmin.managers.ImpersonationManager;
import edu.mines.gradingadmin.models.Course;
import edu.mines.gradingadmin.models.User;
import edu.mines.gradingadmin.models.tasks.CourseSyncTaskDef;
import edu.mines.gradingadmin.repositories.*;
import edu.mines.gradingadmin.seeders.CanvasSeeder;
import edu.mines.gradingadmin.repositories.CourseRepo;
import edu.mines.gradingadmin.seeders.CourseSeeders;
import edu.mines.gradingadmin.seeders.UserSeeders;
import edu.mines.gradingadmin.services.external.CanvasService;
import edu.mines.gradingadmin.services.external.PolicyServerService;
import edu.mines.gradingadmin.services.external.S3Service;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;

import java.util.*;
import java.util.List;
import java.util.Optional;

@SpringBootTest
@Transactional
public class TestCourseService implements PostgresTestContainer, CanvasSeeder, MinioTestContainer {

    @Autowired
    private CourseSeeders courseSeeders;

    @Autowired
    private CourseRepo courseRepo;

    @Autowired
    private CourseLateRequestConfigRepo lateRequestConfigRepo;

    private CourseService courseService;
    @Mock
    private CanvasService canvasService;

    @Autowired
    private UserSeeders userSeeders;

    @Autowired
    private ImpersonationManager impersonationManager;

    @Autowired
    private S3Service s3Service;

    @Autowired
    private ScheduledTaskRepo<CourseSyncTaskDef> scheduledTaskRepo;

    @BeforeAll
    static void setupClass() {
        postgres.start();
        minio.start();
    }

    @BeforeEach
    void setup(){
        courseService = new CourseService(
                courseRepo, lateRequestConfigRepo, scheduledTaskRepo,
                Mockito.mock(ApplicationEventPublisher.class),
                impersonationManager, canvasService,
                s3Service

        );

        applyMocks(canvasService);
    }


    @AfterEach
    void tearDown() {
        courseSeeders.clearAll();
        userSeeders.clearAll();
    }

    @Test
    void verifyGetCourse() {
        Course course1 = courseSeeders.course1();
        Optional<Course> course = courseService.getCourse(course1.getId());

        Assertions.assertTrue(course.isPresent());
        Assertions.assertEquals(course.get(), course1);
    }

    @Test
    void verifyGetCourseNotFound() {
        Optional<Course> course = courseService.getCourse(UUID.randomUUID());
        Assertions.assertTrue(course.isEmpty());
    }

    @Test
    void verifyGetCourseIncludeAll() {
        Course seededCourse = courseSeeders.populatedCourse();

        Course course = courseService.getCourse(seededCourse.getId()).orElseThrow(AssertionError::new);

        Assertions.assertEquals(1, course.getAssignments().size());
        Assertions.assertEquals(1, course.getSections().size());
        Assertions.assertEquals(1, course.getMembers().size());
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
    void verifySyncCourseWithCanvas(){
//        Course course = courseSeeders.course1();
//        User admin = userSeeders.admin1();
//
//        SyncCourseTaskDef taskDef = new SyncCourseTaskDef();
//        taskDef.setCreatedByUser(admin);
//        taskDef.setCourseToImport(course.getId());
//        taskDef.setCanvasId(course1Id);
//        taskDef.setOverwriteCode(true);
//        taskDef.setOverwriteName(true);
//
//        courseService.syncCourseTask(taskDef);
//
//        course = courseService.getCourse(course.getId()).orElseThrow(AssertionError::new);
//
//        Assertions.assertEquals(course1.get().getCourseCode(), course.getCode());
//        Assertions.assertEquals(course1.get().getName(), course.getName());
    }

    @Test
    void verifyImportCourseFromCanvas(){
        Course course = courseSeeders.course1();
        User admin = userSeeders.admin1();

        CourseSyncTaskDef taskDef = new CourseSyncTaskDef();
        taskDef.setCreatedByUser(admin);
        taskDef.setCourseToSync(course.getId());
        taskDef.setCanvasId(course1Id);
        taskDef.shouldOverwriteCode(true);
        taskDef.shouldOverwriteName(true);

        courseService.syncCourseTask(taskDef);

        course = courseService.getCourse(course.getId()).orElseThrow(AssertionError::new);

        Assertions.assertEquals(course1.get().getCourseCode(), course.getCode());
        Assertions.assertEquals(course1.get().getName(), course.getName());
    }

    @Test
    void verifyCourseDoesNotExist(){
        Optional<Course> course = courseService.createNewCourse(new CourseDTO().name("Test Course 1").term("Fall 2024").code("fall.2024.tc.1"));
        Assertions.assertTrue(course.isPresent());
        Assertions.assertNotNull(course.get().getId());
    }

    @Test
    void verifyNewCourseHasName(){
        Optional<Course> course = courseService.createNewCourse(new CourseDTO().name("Another Test Course 1").term("Spring 2025").code("spring.2025.atc.1"));
        Assertions.assertTrue(course.isPresent());
        Assertions.assertEquals("Another Test Course 1", course.get().getName());
    }

    @Test
    void verifyNewCourseHasTerm(){
        Optional<Course> course = courseService.createNewCourse(new CourseDTO().name("Another Test Course 1").term("Spring 2025").code("spring.2025.atc.1"));
        Assertions.assertTrue(course.isPresent());
        Assertions.assertEquals("Spring 2025", course.get().getTerm());
    }

    @Test
    void verifyNewCourseHasCode(){
        Optional<Course> course = courseService.createNewCourse(new CourseDTO().name("Test Course 1").term("Fall 2024").code("fall.2024.tc.1"));
        Assertions.assertTrue(course.isPresent());
        Assertions.assertEquals("fall.2024.tc.1", course.get().getCode());
    }

    @Test
    void verifyEnableCourse() {
        Course inactiveCourse = courseSeeders.course2();
        Assertions.assertFalse(inactiveCourse.isEnabled());
        courseService.enableCourse(inactiveCourse.getId());
        Assertions.assertTrue(inactiveCourse.isEnabled());
    }

    @Test
    void verifyDisableCourse() {
        Course activeCourse = courseSeeders.course1();
        Assertions.assertTrue(activeCourse.isEnabled());
        courseService.disableCourse(activeCourse.getId());
        Assertions.assertFalse(activeCourse.isEnabled());
    }

    @Test
    void verifyWhenAlreadyEnabledDisabled() {
        Course activeCourse = courseSeeders.course1();
        Course inactiveCourse = courseSeeders.course2();
        courseService.enableCourse(activeCourse.getId());
        courseService.disableCourse(inactiveCourse.getId());

        Assertions.assertTrue(activeCourse.isEnabled());
        Assertions.assertFalse(inactiveCourse.isEnabled());
    }
}
