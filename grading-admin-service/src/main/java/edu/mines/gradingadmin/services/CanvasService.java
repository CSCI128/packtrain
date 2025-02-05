package edu.mines.gradingadmin.services;


import edu.ksu.canvas.CanvasApiFactory;
import edu.ksu.canvas.interfaces.CourseReader;
import edu.ksu.canvas.interfaces.SectionReader;
import edu.ksu.canvas.interfaces.UserReader;
import edu.ksu.canvas.model.Course;
import edu.ksu.canvas.model.Section;
import edu.ksu.canvas.model.User;
import edu.ksu.canvas.oauth.NonRefreshableOauthToken;
import edu.ksu.canvas.oauth.OauthToken;
import edu.ksu.canvas.requestOptions.GetSingleCourseOptions;
import edu.ksu.canvas.requestOptions.GetUsersInCourseOptions;
import edu.ksu.canvas.requestOptions.ListCurrentUserCoursesOptions;
import edu.mines.gradingadmin.config.EndpointConfig;
import edu.mines.gradingadmin.managers.SecurityManager;
import edu.mines.gradingadmin.models.CredentialType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class CanvasService {
    private final SecurityManager manager;
    private final EndpointConfig.CanvasConfig config;
    private final CanvasApiFactory canvasApiFactory;

    public CanvasService(SecurityManager manager, EndpointConfig.CanvasConfig config) {
        this.manager = manager;
        this.config = config;

        canvasApiFactory = new CanvasApiFactory(this.config.getEndpoint().toString());
    }

    public List<Course> getAllAvailableCourses(){
        OauthToken canvasToken = new NonRefreshableOauthToken(manager.getCredential(CredentialType.CANVAS, UUID.randomUUID()));

        CourseReader reader = canvasApiFactory.getReader(CourseReader.class, canvasToken);

        ListCurrentUserCoursesOptions params = new ListCurrentUserCoursesOptions()
                .withEnrollmentType(ListCurrentUserCoursesOptions.EnrollmentType.TEACHER);

        List<Course> courses = List.of();
        try {
            courses = reader.listCurrentUserCourses(params);
        } catch (IOException e) {
            log.error("Failed to get courses from Canvas");
        }

        return courses;
    }

    public Optional<Course> getCourse(String id){
        OauthToken canvasToken = new NonRefreshableOauthToken(manager.getCredential(CredentialType.CANVAS, UUID.randomUUID()));

        CourseReader reader = canvasApiFactory.getReader(CourseReader.class, canvasToken);

        Optional<Course> course = Optional.empty();

        GetSingleCourseOptions params = new GetSingleCourseOptions(id);

        try {
            course = reader.getSingleCourse(params);
        } catch (IOException e) {
            log.error("Failed to get course from Canvas");
        }

        return course;
    }

    public List<User> getCourseMembers(String id){
        OauthToken canvasToken = new NonRefreshableOauthToken(manager.getCredential(CredentialType.CANVAS, UUID.randomUUID()));

        UserReader reader = canvasApiFactory.getReader(UserReader.class, canvasToken);

        List<User> users = List.of();

        GetUsersInCourseOptions params = new GetUsersInCourseOptions(id)
                .enrollmentState(List.of(GetUsersInCourseOptions.EnrollmentState.ACTIVE))
                .enrollmentType(List.of(GetUsersInCourseOptions.EnrollmentType.TEACHER, GetUsersInCourseOptions.EnrollmentType.STUDENT))
                .include(List.of(GetUsersInCourseOptions.Include.ENROLLMENTS));

        try {
            // omg i love you KSU.
            // automatic handling of result pages <3
            users = reader.getUsersInCourse(params);
        } catch (IOException e){
            log.error("Failed to get users from in course from Canvas");
        }

        return users;
    }

    public List<Section> getCourseSections(String id){
        OauthToken canvasToken = new NonRefreshableOauthToken(manager.getCredential(CredentialType.CANVAS, UUID.randomUUID()));

        SectionReader reader = canvasApiFactory.getReader(SectionReader.class, canvasToken);

        List<Section> sections = List.of();

        try {
            // omg i love you KSU.
            // automatic handling of result pages <3
            sections = reader.listCourseSections(id, List.of());
        } catch (IOException e){
            log.error("Failed to get sections for course from Canvas");
        }

        return sections;
    }

}