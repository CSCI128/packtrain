package edu.mines.gradingadmin.seeders;

import edu.mines.gradingadmin.models.Course;
import edu.mines.gradingadmin.repositories.CourseRepo;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
//@Profile("test")
public class CourseSeeders {
    private final CourseRepo repo;

    public CourseSeeders(CourseRepo repo) {
        this.repo = repo;
    }


    public Course course1(){
        Course course = new Course();
        course.setTerm("FALL 2001");
        course.setEnabled(true);
        course.setName("Test Course 1");
        course.setCode("fall.2001.tc.1");

        return repo.save(course);
    }

    public void clearAll(){
        repo.deleteAll();
    }
}
