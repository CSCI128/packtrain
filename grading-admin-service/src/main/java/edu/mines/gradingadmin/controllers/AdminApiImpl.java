package edu.mines.gradingadmin.controllers;

import edu.mines.gradingadmin.api.AdminApiDelegate;
import edu.mines.gradingadmin.managers.SecurityManager;
import edu.mines.gradingadmin.models.Course;
import edu.mines.gradingadmin.services.CourseService;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AdminApiImpl implements AdminApiDelegate {

    private final CourseService courseService;
    private final SecurityManager securityManager;

    public AdminApiImpl(CourseService courseService, SecurityManager securityManager) {
        this.courseService = courseService;
        this.securityManager = securityManager;
    }

    @Override
    public ResponseEntity<List<edu.mines.gradingadmin.data.Course>> getCourses(Boolean active) {
        Optional<List<Course>> courses = courseService.getCourses(active);

        List<edu.mines.gradingadmin.data.Course> coursesResponse = new ArrayList<>();
        if(courses.isPresent()) {
            for(Course course : courses.get()) {
                edu.mines.gradingadmin.data.Course courseDto = new edu.mines.gradingadmin.data.Course();

                courseDto.setId(String.valueOf(course.getId()));
                courseDto.setTerm(course.getTerm());
                courseDto.setEnabled(course.isEnabled());
                courseDto.setName(course.getName());
                courseDto.setCode(course.getCode());

                coursesResponse.add(courseDto);
            }
        }
        return ResponseEntity.ok(coursesResponse);
    }
}
