package edu.mines.gradingadmin.seeders;

import edu.mines.gradingadmin.models.Course;
import edu.mines.gradingadmin.repositories.CourseRepo;
import org.springframework.stereotype.Service;

@Service
public class CourseSeeders {
    private final CourseRepo repo;

    public CourseSeeders(CourseRepo repo) {
        this.repo = repo;
    }

    public Course course1() {
        Course course = new Course();
        course.setTerm("FALL 2001");
        course.setEnabled(true);
        course.setName("Test Course 1");
        course.setCode("fall.2001.tc.1");

        return repo.save(course);
    }

    public Course course2() {
        Course course = new Course();
        course.setTerm("FALL 2002");
        course.setEnabled(false);
        course.setName("Test Course 2");
        course.setCode("fall.2002.tc.2");

        return repo.save(course);
    }

    public void clearAll(){
        repo.deleteAll();
    }
}
