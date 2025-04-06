package edu.mines.gradingadmin.seeders;

import edu.mines.gradingadmin.models.Course;
import edu.mines.gradingadmin.models.CourseMember;
import edu.mines.gradingadmin.models.Section;
import edu.mines.gradingadmin.models.User;
import edu.mines.gradingadmin.models.enums.CourseRole;
import edu.mines.gradingadmin.repositories.CourseMemberRepo;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.Set;

@Service
public class CourseMemberSeeder {
    private final CourseMemberRepo courseMemberRepo;

    public CourseMemberSeeder(CourseMemberRepo courseMemberRepo) {
        this.courseMemberRepo = courseMemberRepo;
    }

    public CourseMember student(User user, Course course, Section section){
        CourseMember member = new CourseMember();
        member.setCourse(course);
        member.setUser(user);
        member.setCanvasId(String.valueOf(new Random().nextLong()));
        member.setRole(CourseRole.STUDENT);
        member.setSections(Set.of(section));

        return courseMemberRepo.save(member);
    }

    public void clearAll() {
        courseMemberRepo.deleteAll();

    }
}
