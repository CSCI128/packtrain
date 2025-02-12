package edu.mines.gradingadmin.seeders;

import edu.ksu.canvas.model.Course;
import edu.ksu.canvas.model.Enrollment;
import edu.ksu.canvas.model.Section;
import edu.ksu.canvas.model.User;
import edu.mines.gradingadmin.services.CanvasService;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.*;

public interface CanvasSeeder {
    long course1Id = 123456L;
    long course1Section1Id = 1L;
    long course2Id = 678901L;
    long course2Section1Id = 2L;
    long course2Section2Id = 3L;


    Supplier<User> instructor1 = () -> {
        long instructor1Id = 52461L;
        var instructor = new User();
        instructor.setId(instructor1Id);
        instructor.setName("Eleanor Instructor");
        instructor.setEmail("eleanor@test.com");
        instructor.setSisUserId("09812324");

        return instructor;
    };

    Supplier<User> instructor2 = () -> {
        long instructor2Id = 72918L;
        var instructor = new User();
        instructor.setId(instructor2Id);
        instructor.setName("Lukas Instructor");
        instructor.setEmail("lukas@test.com");
        instructor.setSisUserId("89134509");

        return instructor;
    };

    Supplier<User> student1 = () -> {
        long student1Id = 87122L;
        var student = new User();
        student.setId(student1Id);
        student.setName("Colin Student");
        student.setEmail("colin@test.com");
        student.setSisUserId("12837456");

        return student;
    };
    Supplier<User> student2 = () -> {
        long student2Id = 65781L;
        var student = new User();
        student.setId(student2Id);
        student.setName("Grace Student");
        student.setEmail("grace@test.com");
        student.setSisUserId("15809516");

        return student;
    };

    Supplier<Course> course1 = () -> {
        var course = new Course();
        course.setName("Test Course 1");
        course.setCourseCode("fall.2001.tc.1");
        course.setTermId("FALL 2001");
        course.setId(course1Id);
        return course;
    };

    Supplier<Course> course2 = () -> {
        var course = new Course();
        course.setName("Test Course 2");
        course.setCourseCode("fall.2001.tc.2");
        course.setTermId("FALL 2001");
        course.setId(course2Id);
        return course;
    };

    Supplier<List<Section>> course1Sections = () -> {
        var section = new Section();
        section.setId(course1Section1Id);
        section.setCourseId(course1Id);
        section.setName("Test Course 1 Section A");

        return List.of(section);
    };

    Supplier<List<Section>> course2Sections = () -> {
        var section1 = new Section();
        section1.setId(course2Section1Id);
        section1.setCourseId(course2Id);
        section1.setName("Test Course 2 Section A");

        var section2 = new Section();
        section2.setId(course2Section2Id);
        section2.setCourseId(course2Id);
        section2.setName("Test Course 2 Section B");

        return List.of(section1, section2);
    };

    Supplier<Map<String, User>> course1Users = () -> {
        var instructorEnrollment = new Enrollment();
        instructorEnrollment.setCourseId(course1Id);
        instructorEnrollment.setCourseSectionId(String.valueOf(course1Section1Id));
        instructorEnrollment.setType("TeacherEnrollment");

        var studentEnrollment = new Enrollment();
        studentEnrollment.setCourseId(course1Id);
        studentEnrollment.setCourseSectionId(String.valueOf(course1Section1Id));
        studentEnrollment.setType("StudentEnrollment");

        var user1 = instructor1.get();
        user1.setEnrollments(List.of(
                instructorEnrollment
        ));

        var user2 = student1.get();
        user2.setEnrollments(List.of(
                studentEnrollment
        ));

        var user3 = student2.get();
        user3.setEnrollments(List.of(
                studentEnrollment
        ));

        return Map.of(
                user1.getSisUserId(), user1,
                user2.getSisUserId(), user2,
                user3.getSisUserId(), user3
        );
    };

    Supplier<Map<String, User>> course2Users = () -> {
        var instructorEnrollment1 = new Enrollment();
        instructorEnrollment1.setCourseId(course2Id);
        instructorEnrollment1.setCourseSectionId(String.valueOf(course2Section1Id));
        instructorEnrollment1.setType("TeacherEnrollment");

        var instructorEnrollment2 = new Enrollment();
        instructorEnrollment2.setCourseId(course2Id);
        instructorEnrollment2.setCourseSectionId(String.valueOf(course2Section2Id));
        instructorEnrollment2.setType("TeacherEnrollment");

        var studentEnrollment1 = new Enrollment();
        studentEnrollment1.setCourseId(course2Id);
        studentEnrollment1.setCourseSectionId(String.valueOf(course2Section1Id));
        studentEnrollment1.setType("StudentEnrollment");

        var studentEnrollment2 = new Enrollment();
        studentEnrollment2.setCourseId(course2Id);
        studentEnrollment2.setCourseSectionId(String.valueOf(course2Section2Id));
        studentEnrollment2.setType("StudentEnrollment");

        var user1 = instructor1.get();
        user1.setEnrollments(List.of(
                instructorEnrollment1, instructorEnrollment2
        ));

        var user2 = instructor2.get();
        user1.setEnrollments(List.of(
                instructorEnrollment1
        ));

        var user3 = student1.get();
        user2.setEnrollments(List.of(
                studentEnrollment1
        ));

        var user4 = student2.get();
        user3.setEnrollments(List.of(
                studentEnrollment2
        ));

        return Map.of(
                user1.getSisUserId(), user1,
                user2.getSisUserId(), user2,
                user3.getSisUserId(), user3,
                user4.getSisUserId(), user4
        );
    };

    /**
     * This function mocks out functions that make API calls in the canvas API.
     * This ensures reproducibility and reliability for tests that rely on canvas service.
     * @param service the canvas service to mock
     */
    default void applyMocks(CanvasService service){
        Mockito.when(service.asUser(any()).getAllAvailableCourses()).thenReturn(List.of(course1.get(), course2.get()));
        Mockito.when(service.asUser(any()).getCourse(anyString())).thenReturn(Optional.empty());
        Mockito.when(service.asUser(any()).getCourse(String.valueOf(course1Id))).thenReturn(Optional.of(course1.get()));
        Mockito.when(service.asUser(any()).getCourse(String.valueOf(course2Id))).thenReturn(Optional.of(course2.get()));
        Mockito.when(service.asUser(any()).getCourseMembers(anyLong())).thenReturn(Map.of());
        Mockito.when(service.asUser(any()).getCourseMembers(course1Id)).thenReturn(course1Users.get());
        Mockito.when(service.asUser(any()).getCourseMembers(course2Id)).thenReturn(course2Users.get());
        Mockito.when(service.asUser(any()).getCourseSections(anyLong())).thenReturn(List.of());
        Mockito.when(service.asUser(any()).getCourseSections(course1Id)).thenReturn(course1Sections.get());
        Mockito.when(service.asUser(any()).getCourseSections(course2Id)).thenReturn(course2Sections.get());

    }




}
