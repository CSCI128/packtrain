package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.containers.PostgresTestContainer;
import edu.mines.gradingadmin.models.Course;
import edu.mines.gradingadmin.repositories.CourseRepo;
import edu.mines.gradingadmin.seeders.CourseSeeders;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
@Transactional
public class TestCourseService implements PostgresTestContainer {

    @Autowired
    CourseSeeders courseSeeders;
    @Autowired
    CourseRepo courseRepo;
    @Autowired
    CourseService courseService;

    @BeforeAll
    static void setupClass() {
        postgres.start();
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
