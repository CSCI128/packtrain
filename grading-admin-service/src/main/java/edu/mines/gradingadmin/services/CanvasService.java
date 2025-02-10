package edu.mines.gradingadmin.services;


import edu.ksu.canvas.CanvasApiFactory;
import edu.ksu.canvas.interfaces.CourseReader;
import edu.ksu.canvas.interfaces.SectionReader;
import edu.ksu.canvas.interfaces.UserReader;
import edu.ksu.canvas.model.Course;
import edu.ksu.canvas.model.Enrollment;
import edu.ksu.canvas.model.Section;
import edu.ksu.canvas.model.User;
import edu.ksu.canvas.oauth.NonRefreshableOauthToken;
import edu.ksu.canvas.oauth.OauthToken;
import edu.ksu.canvas.requestOptions.GetSingleCourseOptions;
import edu.ksu.canvas.requestOptions.GetUsersInCourseOptions;
import edu.ksu.canvas.requestOptions.ListCurrentUserCoursesOptions;
import edu.mines.gradingadmin.config.EndpointConfig;
import edu.mines.gradingadmin.managers.SecurityManager;
import edu.mines.gradingadmin.models.CourseRole;
import edu.mines.gradingadmin.models.CredentialType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

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

        log.info("Retrieving available courses for user '{}'.", manager.getUser().getEmail());

        CourseReader reader = canvasApiFactory.getReader(CourseReader.class, canvasToken);

        ListCurrentUserCoursesOptions params = new ListCurrentUserCoursesOptions()
                .withEnrollmentType(ListCurrentUserCoursesOptions.EnrollmentType.TEACHER);

        List<Course> courses = List.of();

        try {
            courses = reader.listCurrentUserCourses(params);
        } catch (IOException e) {
            log.error("Failed to get courses from Canvas");
        }

        log.info("Retrieved {} courses for user '{}'.", courses.size(), manager.getUser().getEmail());

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

    @Cacheable("canvas_users")
    public Map<String, User> getCourseMembers(long id){
        OauthToken canvasToken = new NonRefreshableOauthToken(manager.getCredential(CredentialType.CANVAS, UUID.randomUUID()));

        log.info("Retrieving users for course '{}'. Note: this may take a while depending on enrollment size of course.", id);


        UserReader reader = canvasApiFactory.getReader(UserReader.class, canvasToken);

        Map<String, User> users = Map.of();

        GetUsersInCourseOptions params = new GetUsersInCourseOptions(String.valueOf(id))
                .enrollmentState(List.of(GetUsersInCourseOptions.EnrollmentState.ACTIVE))
                .enrollmentType(List.of(GetUsersInCourseOptions.EnrollmentType.TEACHER, GetUsersInCourseOptions.EnrollmentType.STUDENT))
                .include(List.of(GetUsersInCourseOptions.Include.ENROLLMENTS));

        try {
            // omg i love you KSU.
            // automatic handling of result pages <3
            users = reader.getUsersInCourse(params).stream().collect(Collectors.toMap(User::getSisUserId, user -> user));
        } catch (IOException e){
            log.error("Failed to get users from in course from Canvas.");
        }

        log.info("Retrieved {} users for course '{}'.", users.size(), id);

        return users;
    }

    public List<Section> getCourseSections(long id){
        OauthToken canvasToken = new NonRefreshableOauthToken(manager.getCredential(CredentialType.CANVAS, UUID.randomUUID()));
        log.info("Retrieving sections for course '{}'.", id);

        SectionReader reader = canvasApiFactory.getReader(SectionReader.class, canvasToken);

        List<Section> sections = List.of();

        try {
            sections = reader.listCourseSections(String.valueOf(id), List.of());
        } catch (IOException e){
            log.error("Failed to get sections for course from Canvas");
        }

        log.info("Retrieved {} sections for course '{}'", sections.size(), id);

        return sections;
    }

    public Optional<CourseRole> mapEnrollmentToRole(Enrollment enrollment){
        // for some reason, java pattern matching does not like non-constant strings
        // while I could hard code the enrollment names, Canvas has already changed it a few times in the past few years
        // so, I don't want to.
        // Also, Mines is allowed to override these names if they want to

        if (enrollment.getType().equals(config.getTeacherEnrollment())){
            return Optional.of(CourseRole.TEACHER);
        }
        else if (enrollment.getType().equals(config.getStudentEnrollment())) {
            return Optional.of(CourseRole.STUDENT);
        }
        else if (enrollment.getType().equals(config.getTaEnrollment())){
            return Optional.of(CourseRole.TA);
        }

        log.error("Unsupported enrollment type '{}'", enrollment);

        return Optional.empty();
    }

}