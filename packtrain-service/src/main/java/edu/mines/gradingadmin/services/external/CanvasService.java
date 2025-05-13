package edu.mines.gradingadmin.services.external;


import edu.ksu.canvas.CanvasApiFactory;
import edu.ksu.canvas.interfaces.*;
import edu.ksu.canvas.model.*;
import edu.ksu.canvas.model.assignment.Assignment;
import edu.ksu.canvas.model.assignment.AssignmentGroup;
import edu.ksu.canvas.oauth.NonRefreshableOauthToken;
import edu.ksu.canvas.oauth.OauthToken;
import edu.ksu.canvas.requestOptions.*;
import edu.mines.gradingadmin.config.ExternalServiceConfig;
import edu.mines.gradingadmin.managers.IdentityProvider;
import edu.mines.gradingadmin.managers.SecurityManager;
import edu.mines.gradingadmin.models.enums.CourseRole;
import edu.mines.gradingadmin.models.enums.CredentialType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.couchbase.CouchbaseProperties;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CanvasService {
    private final SecurityManager manager;
    private final ExternalServiceConfig.CanvasConfig config;
    private CanvasApiFactory canvasApiFactory;

    public static class BuiltAssignmentSubmissions {
        private final Map<String, MultipleSubmissionsOptions.StudentSubmissionOption> map;

        private final MultipleSubmissionsOptions multipleSubmissionsOptions;

        protected BuiltAssignmentSubmissions(String courseId, long assignmentId) {
            map = new HashMap<>();
            multipleSubmissionsOptions = new MultipleSubmissionsOptions(courseId, assignmentId, null);
        }

        public BuiltAssignmentSubmissions addSubmission(String canvasId, String comment, double score, boolean excuseStudent){
            if (map.containsKey(canvasId)){
                log.warn("overwriting score for student '{}'", canvasId);
            }

            map.put(canvasId, multipleSubmissionsOptions.createStudentSubmissionOption(comment, String.valueOf(score), excuseStudent, null, null, null));

            return this;
        }

        protected MultipleSubmissionsOptions build(){
            multipleSubmissionsOptions.setStudentSubmissionOptionMap(map);
            return multipleSubmissionsOptions;
        }
    }

    public static class CanvasServiceWithAuth {
        private final CanvasApiFactory canvasApiFactory;
        private final IdentityProvider identityProvider;

        protected CanvasServiceWithAuth(CanvasApiFactory canvasApiFactory, IdentityProvider identityProvider) {
            this.canvasApiFactory = canvasApiFactory;
            this.identityProvider = identityProvider;
        }

        public List<Course> getAllAvailableCourses(){
            OauthToken canvasToken = new NonRefreshableOauthToken(identityProvider.getCredential(CredentialType.CANVAS, UUID.randomUUID()));

            log.info("Retrieving available courses for user '{}'.", identityProvider.getUser().getEmail());

            CourseReader reader = canvasApiFactory.getReader(CourseReader.class, canvasToken);

            ListCurrentUserCoursesOptions params = new ListCurrentUserCoursesOptions()
                    .withEnrollmentType(ListCurrentUserCoursesOptions.EnrollmentType.TEACHER);

            List<Course> courses = List.of();

            try {
                courses = reader.listCurrentUserCourses(params);
            } catch (IOException e) {
                log.error("Failed to get courses from Canvas");
            }

            log.info("Retrieved {} courses for user '{}'.", courses.size(), identityProvider.getUser().getEmail());

            return courses;
        }

        public Optional<Course> getCourse(String id){
            OauthToken canvasToken = new NonRefreshableOauthToken(identityProvider.getCredential(CredentialType.CANVAS, UUID.randomUUID()));

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
            OauthToken canvasToken = new NonRefreshableOauthToken(identityProvider.getCredential(CredentialType.CANVAS, UUID.randomUUID()));

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
            OauthToken canvasToken = new NonRefreshableOauthToken(identityProvider.getCredential(CredentialType.CANVAS, UUID.randomUUID()));
            log.info("Retrieving sections for course '{}'.", id);

            SectionReader reader = canvasApiFactory.getReader(SectionReader.class, canvasToken);

            List<Section> sections = List.of();

            try {
                sections = reader.listCourseSections(String.valueOf(id), List.of());
            } catch (IOException e){
                log.error("Failed to get sections for course from Canvas", e);
            }

            log.info("Retrieved {} sections for course '{}'", sections.size(), id);

            return sections;
        }

        public Map<Long, String> getAssignmentGroups(long id){
            OauthToken canvasToken = new NonRefreshableOauthToken(identityProvider.getCredential(CredentialType.CANVAS, UUID.randomUUID()));
            log.info("Retrieving Assignments Groups for course '{}'.", id);

            AssignmentGroupReader reader = canvasApiFactory.getReader(AssignmentGroupReader.class, canvasToken);


            Map<Long, String> assignmentGroups = Map.of();

            try {
                assignmentGroups = reader.listAssignmentGroup(new ListAssignmentGroupOptions(String.valueOf(id))).stream().collect(Collectors.toMap(AssignmentGroup::getId, AssignmentGroup::getName));

            } catch (IOException e){
                log.error("Failed to get assignment groups for course from Canvas", e);
            }

            log.info("Retrieved {} assignment groups for course '{}'", assignmentGroups.size(), id);

            return assignmentGroups;
        }

        public List<Assignment> getCourseAssignments(long id){
            OauthToken canvasToken = new NonRefreshableOauthToken(identityProvider.getCredential(CredentialType.CANVAS, UUID.randomUUID()));
            log.info("Retrieving Assignments for course '{}'. This will take a while depending on the number of assignments in the course.", id);

            AssignmentReader reader = canvasApiFactory.getReader(AssignmentReader.class, canvasToken);

            List<Assignment> assignments = List.of();

            try {
                // group category id is set iff it's a group assignment
                assignments = reader.listCourseAssignments(new ListCourseAssignmentsOptions(String.valueOf(id)));
            } catch (IOException e) {
                log.error("Failed to get assignments for course from Canvas", e);
            }

            log.info("Retrieved {} assignments for course '{}'", assignments.size(), id);

            return assignments;
        }


        public Optional<Progress> publishCanvasScores(BuiltAssignmentSubmissions submissions){
            OauthToken canvasToken = new NonRefreshableOauthToken(identityProvider.getCredential(CredentialType.CANVAS, UUID.randomUUID()));

            MultipleSubmissionsOptions builtSubmissions = submissions.build();

            log.info("Preparing to publish {} submissions to Canvas", builtSubmissions.getStudentSubmissionOptionMap().size());

            SubmissionWriter writer = canvasApiFactory.getWriter(SubmissionWriter.class, canvasToken);

            Optional<Progress> progress = Optional.empty();

            try {
                progress = writer.gradeMultipleSubmissionsByCourse(builtSubmissions);

            } catch (IOException e){
                log.error("Failed to publish scores!", e);
            }

            if (progress.isEmpty()){
                log.error("Failed to publish scores! Canvas async task service failed to register incoming gradebook changes!");
                return Optional.empty();
            }

            log.info("Gradebook changes acknowledged by Canvas async task service. See '{}' for real time updates.", progress.get().getUrl());


            return progress;
        }

        public boolean isAsyncTaskDone(long asyncTaskId){
            OauthToken canvasToken = new NonRefreshableOauthToken(identityProvider.getCredential(CredentialType.CANVAS, UUID.randomUUID()));

            ProgressReader reader = canvasApiFactory.getReader(ProgressReader.class, canvasToken);

            Optional<Progress> progress = Optional.empty();

            try {
                progress = reader.getProgress(asyncTaskId);
            } catch (IOException e) {
                log.error("Failed to get progress status!", e);
            }

            if(progress.isEmpty()){
                log.error("No progress object was returned! Marking as complete.");
                return true;
            }

            log.trace("Current progress: {}", progress.get().getCompletion());

            return progress.get().getWorkflowState().equals("completed");
        }

    }

    public CanvasService(@Autowired(required = false) SecurityManager manager, ExternalServiceConfig.CanvasConfig config) {
        this.manager = manager;
        this.config = config;

        if (!config.isEnabled()){
            return;
        }

        canvasApiFactory = new CanvasApiFactory(this.config.getEndpoint().toString());
    }

    public CanvasServiceWithAuth withRequestIdentity(){
        if (!config.isEnabled()){
            throw new ExternalServiceDisabledException("Canvas service is disabled!");
        }

        if (manager == null){
            throw new RuntimeException("No request in scope");
        }
        return new CanvasServiceWithAuth(canvasApiFactory, manager);
    }

    public CanvasServiceWithAuth asUser(IdentityProvider identityProvider){
        if (!config.isEnabled()){
            throw new ExternalServiceDisabledException("Canvas service is disabled!");
        }
        return new CanvasServiceWithAuth(canvasApiFactory, identityProvider);
    }

    public BuiltAssignmentSubmissions prepCanvasSubmissionsForPublish(String canvasCourseId, long canvasAssignmentId){
        return new BuiltAssignmentSubmissions(canvasCourseId, canvasAssignmentId);
    }

    public Optional<CourseRole> mapEnrollmentToRole(Enrollment enrollment){
        // for some reason, java pattern matching does not like non-constant strings
        // while I could hard code the enrollment names, Canvas has already changed it a few times in the past few years
        // so, I don't want to.
        // Also, Mines is allowed to override these names if they want to

        if (enrollment.getType().equals(config.getTeacherEnrollment())){
            return Optional.of(CourseRole.INSTRUCTOR);
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