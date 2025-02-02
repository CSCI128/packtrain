package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.models.Course;
import edu.mines.gradingadmin.repositories.CourseRepo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CourseService {

    private final CourseRepo courseRepo;

    public CourseService(CourseRepo courseRepo) {
        this.courseRepo = courseRepo;
    }

    public Optional<List<Course>> getCourses(Boolean active) {
        if(active) {
            return Optional.of(courseRepo.getAll().stream().filter(Course::isEnabled).toList());
        }
        return Optional.of(courseRepo.getAll());
    }
}
