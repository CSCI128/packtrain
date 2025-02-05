package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.models.Course;
import edu.mines.gradingadmin.models.Section;
import edu.mines.gradingadmin.repositories.CourseRepo;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CourseService {

    private final CourseRepo courseRepo;

    private final CanvasService canvasService;

    private final SectionService sectionService;

    public CourseService(CourseRepo courseRepo, CanvasService canvasService, SectionService sectionService) {
        this.courseRepo = courseRepo;
        this.canvasService = canvasService;
        this.sectionService = sectionService;
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
        newCourse = courseRepo.save(newCourse);

        List<Section> sections = sectionService.createSectionsForCourse(newCourse);
        newCourse.setSections(new HashSet<>(sections));


        return Optional.of(newCourse);
    }

}
