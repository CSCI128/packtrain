package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.models.Course;
import edu.mines.gradingadmin.repositories.CourseRepo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CourseService {

    private final CourseRepo courseRepo;

    private final CanvasService canvasService;

    public CourseService(CourseRepo courseRepo, CanvasService canvasService) {
        this.courseRepo = courseRepo;
        this.canvasService = canvasService;
    }

    public List<Course> getCourses(boolean enabled) {
        if(enabled) {
            return courseRepo.getAll(enabled);
        }
        return courseRepo.getAll();
    }

    public Optional<Course> createNewCourse(String canvasId){
        List<edu.ksu.canvas.model.Course> availableCourses =
                canvasService.getAllAvailableCourses()
                    .stream()
                    .filter(course -> course.getId().toString().equals(canvasId))
                    .toList();

        if (availableCourses.isEmpty()){
            return Optional.empty();
        }

        if (availableCourses.size() != 1){
            return Optional.empty();
        }

        edu.ksu.canvas.model.Course canvasCourse = availableCourses.getFirst();

        Course newCourse = new Course();
        newCourse.setCanvasId(canvasCourse.getId().toString());
        newCourse.setCode(canvasCourse.getCourseCode());
        newCourse.setName(canvasCourse.getName());
        newCourse.setTerm(canvasCourse.getTermId());
        newCourse.setEnabled(true);

        return Optional.of(courseRepo.save(newCourse));
    }
}
