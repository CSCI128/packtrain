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
import edu.mines.gradingadmin.services.external.S3Service;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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

    @Autowired
    private GradescopeConfigRepo gradescopeConfigRepo;

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
    @Autowired
    private UserService userService;
    @Autowired
    private PolicyRepo policyRepo;

    @BeforeAll
    static void setupClass() {
        postgres.start();
        minio.start();
    }

    @BeforeEach
    void setup(){
        courseService = new CourseService(
                courseRepo, lateRequestConfigRepo, gradescopeConfigRepo, scheduledTaskRepo,
                Mockito.mock(ApplicationEventPublisher.class),
                impersonationManager, canvasService,
                s3Service, userService
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
        Course course = courseService.getCourse(course1.getId());

        Assertions.assertEquals(course, course1);
    }

    @Test
    void verifyGetCourseNotFound() {
        Assertions.assertThrows(ResponseStatusException.class, () -> {
            courseService.getCourse(UUID.randomUUID());
        });
    }

    @Test
    void verifyGetCourseIncludeAll() {
        Course seededCourse = courseSeeders.populatedCourse();

        Course course = courseService.getCourse(seededCourse.getId());

        Assertions.assertEquals(1, course.getAssignments().size());
        Assertions.assertEquals(1, course.getSections().size());
        Assertions.assertEquals(1, course.getMembers().size());
    }

    @Test
    void verifyGetAllCoursesActive() {
        Course activeCourse = courseSeeders.course1();
        Course inactiveCourse = courseSeeders.course2();

        List<Course> courses = courseService.getAllCourses(true);
        Assertions.assertEquals(1, courses.size());
        Assertions.assertTrue(courses.contains(activeCourse));
        Assertions.assertFalse(courses.contains(inactiveCourse));
    }

    @Test
    void verifyGetAllCoursesIncludingInactive() {
        Course activeCourse = courseSeeders.course1();
        Course inactiveCourse = courseSeeders.course2();

        List<Course> courses = courseService.getAllCourses(false);
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

        course = courseService.getCourse(course.getId());

        Assertions.assertEquals(course1.get().getCourseCode(), course.getCode());
        Assertions.assertEquals(course1.get().getName(), course.getName());
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

    @Test
    void verifyGetAllCourseStudent(){
        Course coursePopulated = courseSeeders.populatedCourse();
        Course course2 = courseSeeders.course2();
        User user = userSeeders.user1();
        List<Course> studentCourses = List.of(coursePopulated);
        List<Course> notStudentCourse = List.of(course2);

        // the user is a member of populatedCourse and not a member of course2
        Assertions.assertEquals(studentCourses, courseService.getCoursesStudent(user));
        Assertions.assertNotEquals(notStudentCourse, courseService.getCoursesStudent(user));

    }

}
