package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.models.Course;
import edu.mines.gradingadmin.repositories.CourseRepo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CourseService {

    private final CourseRepo courseRepo;

    public CourseService(CourseRepo courseRepo) {
        this.courseRepo = courseRepo;
    }

    public List<Course> getCourses(boolean onlyActive) {
        if(onlyActive) {
            return courseRepo.getAll(true);
        }
        return courseRepo.getAll();
    }

    public void enableCourse(UUID courseId) {
        Optional<Course> course = courseRepo.findById(courseId);
        if(course.isPresent()) {
            course.get().setEnabled(true);
            courseRepo.save(course.get());
        }
    }

    public void disableCourse(UUID courseId) {
        Optional<Course> course = courseRepo.findById(courseId);
        if(course.isPresent()) {
            course.get().setEnabled(false);
            courseRepo.save(course.get());
        }
    }
}
