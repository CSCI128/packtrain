package edu.mines.gradingadmin.services;


import edu.ksu.canvas.CanvasApiFactory;
import edu.ksu.canvas.interfaces.CourseReader;
import edu.ksu.canvas.oauth.NonRefreshableOauthToken;
import edu.ksu.canvas.oauth.OauthToken;
import edu.ksu.canvas.requestOptions.GetSingleCourseOptions;
import edu.ksu.canvas.requestOptions.ListCurrentUserCoursesOptions;
import edu.mines.gradingadmin.data.Course;
import edu.mines.gradingadmin.managers.SecurityManager;
import edu.mines.gradingadmin.models.ExternalSource;
import edu.mines.gradingadmin.models.ExternalSourceType;
import edu.mines.gradingadmin.repositories.ExternalSourceRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class CanvasService {
    private final boolean enabled;
    private final SecurityManager manager;
    private final CanvasApiFactory canvasApiFactory;
    private final String baseEndpoint;

    public CanvasService(SecurityManager manager, ExternalSourceRepo externalSourceRepo) {
        this.manager = manager;

        Optional<ExternalSource> source = externalSourceRepo.getByType(ExternalSourceType.CANVAS);

        enabled = source.isPresent();

        baseEndpoint = enabled ? source.get().getEndpoint() : "disabled";

        canvasApiFactory = new CanvasApiFactory(baseEndpoint);
    }

    public List<Course> getAllAvailableCourses(){
        OauthToken canvasToken = new NonRefreshableOauthToken(manager.getCredential(baseEndpoint, UUID.randomUUID()));

        CourseReader reader = canvasApiFactory.getReader(CourseReader.class, canvasToken);

        ListCurrentUserCoursesOptions params = new ListCurrentUserCoursesOptions()
                .withEnrollmentType(ListCurrentUserCoursesOptions.EnrollmentType.TEACHER);

        List<edu.ksu.canvas.model.Course> courses = List.of();
        try {
            courses = reader.listCurrentUserCourses(params);
        } catch (IOException e) {
            log.error("Failed to get courses from Canvas");
        }

        return courses.stream()
                .map(
                course -> new Course()
                        .code(course.getCourseCode())
                        .name(course.getName())
                        .canvasId(course.getId().toString())
                )
                .toList();
    }

    public Course getCourse(String id){
        OauthToken canvasToken = new NonRefreshableOauthToken(manager.getCredential(baseEndpoint, UUID.randomUUID()));

        CourseReader reader = canvasApiFactory.getReader(CourseReader.class, canvasToken);

        Optional<edu.ksu.canvas.model.Course> course = Optional.empty();

        GetSingleCourseOptions params = new GetSingleCourseOptions(id)
                .includes(List.of(GetSingleCourseOptions.Include.SECTIONS));

        try {
            course = reader.getSingleCourse(params);
//        } catch (IOException e) {
            log.error("Failed to get course from Canvas");
        }

    }





}