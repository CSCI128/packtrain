package edu.mines.gradingadmin.seeders;

import edu.mines.gradingadmin.models.*;
import edu.mines.gradingadmin.models.enums.CourseRole;
import edu.mines.gradingadmin.repositories.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;

@Service
public class CourseSeeders {
    private final CourseRepo repo;
    private final CourseMemberRepo courseMemberRepo;
    private final SectionRepo sectionRepo;
    private final UserSeeders userSeeders;
    private final AssignmentRepo assignmentRepo;

    public CourseSeeders(CourseRepo repo, CourseMemberRepo courseMemberRepo, SectionRepo sectionRepo, UserSeeders userSeeders, AssignmentRepo assignmentRepo) {
        this.repo = repo;
        this.courseMemberRepo = courseMemberRepo;
        this.sectionRepo = sectionRepo;
        this.userSeeders = userSeeders;
        this.assignmentRepo = assignmentRepo;
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
        Course course = course1();

        // seed section
        Section section = new Section();
        section.setName("Section A");
        section.setCourse(course);
        section = sectionRepo.save(section);
        course.setSections(Set.of(section));

        User user = userSeeders.user1();

        // seed member
        CourseMember member = new CourseMember();
        member.setRole(CourseRole.STUDENT);
        member.setCanvasId("999999");
        member.setUser(user);
        member.setCourse(course);
        member.setSections(Set.of(section));
        member = courseMemberRepo.save(member);
        course.setMembers(Set.of(member));

        Assignment assignment = new Assignment();
        assignment.setCourse(course);
        assignment.setName("Test Assignment");
        assignment.setCategory("Assessments");
        assignment.setPoints(25.0);
        assignment.setDueDate(Instant.now());
        assignment.setUnlockDate(Instant.ofEpochSecond(946684800));
        assignment.setEnabled(true);
        assignmentRepo.save(assignment);
        course.setAssignments(Set.of(assignment));

        return course;
    }

    public Section section(Course course){
        Section section = new Section();
        section.setName("Section A");
        section.setCourse(course);

        return sectionRepo.save(section);
    }

    public void clear(){
        sectionRepo.deleteAll();
        repo.deleteAll();
    }

    public void clearAll(){
        courseMemberRepo.deleteAll();
        sectionRepo.deleteAll();
        assignmentRepo.deleteAll();
        repo.deleteAll();
    }
}
