package edu.mines.gradingadmin.controllers;

import edu.mines.gradingadmin.api.AdminApiDelegate;
import edu.mines.gradingadmin.managers.SecurityManager;
import edu.mines.gradingadmin.models.Course;
import edu.mines.gradingadmin.services.CourseService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
public class AdminApiImpl implements AdminApiDelegate {

    private final CourseService courseService;
    private final SecurityManager securityManager;

    public AdminApiImpl(CourseService courseService, SecurityManager securityManager) {
        this.courseService = courseService;
        this.securityManager = securityManager;
    }

    @Override
    public ResponseEntity<edu.mines.gradingadmin.data.Course> newCourse(String canvasId) {
        Optional<Course> course = courseService.createNewCourse(canvasId);

        if (course.isEmpty()){
            return ResponseEntity.notFound().build();
        }

        var courseRes = new edu.mines.gradingadmin.data.Course()
                .id(course.get().getCanvasId())
                .canvasId(course.get().getCanvasId())
                .code(course.get().getCode())
                .enabled(course.get().isEnabled())
                .name(course.get().getName());

        return ResponseEntity.ok(courseRes);

    }

    @Override
    public ResponseEntity<List<edu.mines.gradingadmin.data.Course>> getCourses(Boolean onlyActive) {
        List<Course> courses = courseService.getCourses(onlyActive);

        List<edu.mines.gradingadmin.data.Course> coursesResponse = courses.stream().map(course ->
                new edu.mines.gradingadmin.data.Course()
                        .id(course.getId().toString())
                        .term(course.getTerm())
                        .enabled(course.isEnabled())
                        .name(course.getName())
                        .code(course.getCode())
        ).toList();

        return ResponseEntity.ok(coursesResponse);
    }

    @Override
    public ResponseEntity<Void> enableCourse(String courseId) {
        courseService.enableCourse(UUID.fromString(courseId));
        return ResponseEntity.accepted().build();
    }

    @Override
    public ResponseEntity<Void> disableCourse(String courseId) {
        courseService.disableCourse(UUID.fromString(courseId));
        return ResponseEntity.accepted().build();
    }
}
