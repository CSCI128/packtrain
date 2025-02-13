package edu.mines.gradingadmin.seeders;

import edu.mines.gradingadmin.models.*;
import edu.mines.gradingadmin.repositories.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;

@Transactional
@Service
public class CourseSeeders {
    private final CourseRepo repo;
    private final CourseMemberRepo courseMemberRepo;
    private final UserRepo userRepo;

    public CourseSeeders(CourseRepo repo, CourseMemberRepo courseMemberRepo, UserRepo userRepo) {
        this.repo = repo;
        this.courseMemberRepo = courseMemberRepo;
        this.userRepo = userRepo;
    }

    public Course course1() {
        return course1(234989);
    }

    public Course course1(long id) {
        Course course = new Course();
        course.setTerm("FALL 2001");
        course.setCanvasId(id);
        course.setEnabled(true);
        course.setName("Test Course 1");
        course.setCode("fall.2001.tc.1");

        return repo.save(course);
    }

    public Course course2() {
        Course course = new Course();
        course.setTerm("FALL 2002");
        course.setCanvasId(928345);
        course.setEnabled(false);
        course.setName("Test Course 2");
        course.setCode("fall.2002.tc.2");

        return repo.save(course);
    }

    public Course populatedCourse() {
        Course course = new Course();

        // seed section
        Section section = new Section();
        section.setName("Section A");
        course.setSections(Set.of(section));

        // seed user
        User user = new User();
        user.setName("User 1");
        user.setEmail("user1@test.com");
        user.setCwid("80000001");
        user.setEnabled(true);

        // seed member
        CourseMember member = new CourseMember();
        member.setUser(user);
        member.setRole(CourseRole.STUDENT);
        member.setCourse(course);
        member.setCanvasId("x");
        courseMemberRepo.save(member);

        course.setMembers(Set.of(member));

        Assignment assignment = new Assignment();
        assignment.setCourse(course);
        assignment.setName("Test Assignment");
        assignment.setCategory("Assessments");
        assignment.setPoints(25.0);
        assignment.setDueDate(Instant.now());
        assignment.setUnlockDate(Instant.now());

        course.setAssignments(Set.of(assignment));

        return repo.save(course);
    }

    public void clearAll(){
        repo.deleteAll();
    }
}
