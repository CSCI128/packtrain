package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.containers.MinioTestContainer;
import edu.mines.gradingadmin.containers.PostgresTestContainer;
import edu.mines.gradingadmin.managers.ImpersonationManager;
import edu.mines.gradingadmin.models.Course;
import edu.mines.gradingadmin.models.User;
import edu.mines.gradingadmin.models.tasks.CourseImportTaskDef;
import edu.mines.gradingadmin.repositories.*;
import edu.mines.gradingadmin.seeders.CanvasSeeder;
import edu.mines.gradingadmin.models.*;
import edu.mines.gradingadmin.repositories.CourseMemberRepo;
import edu.mines.gradingadmin.repositories.CourseRepo;
import edu.mines.gradingadmin.repositories.SectionRepo;
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

import java.time.LocalDateTime;
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
    private CourseMemberRepo courseMemberRepo;

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
    private PolicyRepo policyRepo;

    @Autowired
    private ScheduledTaskRepo<CourseImportTaskDef> scheduledTaskRepo;

    @BeforeAll
    static void setupClass() {
        postgres.start();
        minio.start();
    }

    @BeforeEach
    void setup(){
        courseService = new CourseService(
                courseRepo, scheduledTaskRepo,
                Mockito.mock(ApplicationEventPublisher.class),
                impersonationManager, canvasService,
                s3Service, policyRepo

        );

        applyMocks(canvasService);
    }


    @AfterEach
    void tearDown() {
        userSeeders.clearAll();
        courseRepo.deleteAll();
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
        // TODO this is a bad test I think
        Optional<Course> course = courseService.getCourse(UUID.randomUUID());
        Assertions.assertTrue(course.isEmpty());
    }

    @Test
    void verifyGetCourseIncludeMembers() {
        User user = userSeeders.user1();
        Course course1 = courseSeeders.course1();

        // seed section
        Section section = new Section();
        section.setName("Section A");

        // seed member
        CourseMember member = new CourseMember();
        member.setUser(user);
        member.setRole(CourseRole.STUDENT);
        member.setSections(Set.of(section));
        member.setCourse(course1);
        member.setCanvasId("x");
        courseMemberRepo.save(member);

        course1.setMembers(Set.of(member));

        Optional<Course> course = courseService.getCourse(course1.getId());

        Assertions.assertTrue(course.isPresent());
        Assertions.assertEquals(1, course.get().getMembers().size());
        Assertions.assertTrue(course.get().getMembers().contains(member));
    }

    @Test
    void verifyGetCourseIncludeAssignments() {
        Course course1 = courseSeeders.course1();

        Assignment assignment = new Assignment();
        assignment.setCourse(course1);
        assignment.setName("Test Assignment");
        assignment.setCategory("Assessments");
        assignment.setPoints(25.0);
        assignment.setDueDate(LocalDateTime.now());
        assignment.setUnlockDate(LocalDateTime.now());

        course1.setAssignments(Set.of(assignment));

        Optional<Course> course = courseService.getCourse(course1.getId());

        Assertions.assertTrue(course.isPresent());
        Assertions.assertEquals(1, course.get().getAssignments().size());
        Assertions.assertTrue(course.get().getAssignments().contains(assignment));
    }

    @Test
    void verifyGetCourseIncludeSections() {
        Course course1 = courseSeeders.course1();
        Optional<Course> course = courseService.getCourse(course1.getId());

        // seed section
        Section section = new Section();
        section.setName("Section A");

        course1.setSections(Set.of(section));

        Assertions.assertTrue(course.isPresent());
        Assertions.assertEquals(1, course.get().getSections().size());
        Assertions.assertTrue(course.get().getSections().contains(section));
    }

    @Test
    void verifyGetCourseIncludeAll() {
        User user = userSeeders.user1();
        Course course1 = courseSeeders.course1();
        Optional<Course> course = courseService.getCourse(course1.getId());

        // seed section
        Section section = new Section();
        section.setName("Section A");
        course1.setSections(Set.of(section));

        // seed member
        CourseMember member = new CourseMember();
        member.setUser(user);
        member.setRole(CourseRole.STUDENT);
        member.setSections(Set.of(section));
        member.setCourse(course1);
        member.setCanvasId("x");
        courseMemberRepo.save(member);

        course1.setMembers(Set.of(member));

        Assignment assignment = new Assignment();
        assignment.setCourse(course1);
        assignment.setName("Test Assignment");
        assignment.setCategory("Assessments");
        assignment.setPoints(25.0);
        assignment.setDueDate(LocalDateTime.now());
        assignment.setUnlockDate(LocalDateTime.now());

        course1.setAssignments(Set.of(assignment));

        Assertions.assertTrue(course.isPresent());
        Assertions.assertEquals(1, course.get().getMembers().size());
        Assertions.assertEquals(1, course.get().getAssignments().size());
        Assertions.assertEquals(1, course.get().getSections().size());
        Assertions.assertTrue(course.get().getMembers().contains(member));
        Assertions.assertTrue(course.get().getAssignments().contains(assignment));
        Assertions.assertTrue(course.get().getSections().contains(section));
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
