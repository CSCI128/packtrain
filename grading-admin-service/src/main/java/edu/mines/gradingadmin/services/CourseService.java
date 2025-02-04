package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.models.Course;
import edu.mines.gradingadmin.repositories.CourseRepo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourseService {

    private final CourseRepo courseRepo;

    public CourseService(CourseRepo courseRepo) {
        this.courseRepo = courseRepo;
    }

    public List<Course> getCourses(boolean onlyActive) {
        if(onlyActive) {
            return courseRepo.getAll().stream().filter(Course::isEnabled).toList();
        }
        return courseRepo.getAll();
    }
}
