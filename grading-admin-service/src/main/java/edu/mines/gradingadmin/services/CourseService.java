package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.config.ExternalServiceConfig;
import edu.mines.gradingadmin.data.CourseDTO;
import edu.mines.gradingadmin.data.CourseLateRequestConfigDTO;
import edu.mines.gradingadmin.data.policyServer.PolicyServerErrorDTO;
import edu.mines.gradingadmin.events.NewTaskEvent;
import edu.mines.gradingadmin.managers.IdentityProvider;
import edu.mines.gradingadmin.managers.ImpersonationManager;
import edu.mines.gradingadmin.models.*;
import edu.mines.gradingadmin.models.tasks.ScheduledTaskDef;
import edu.mines.gradingadmin.models.tasks.CourseSyncTaskDef;
import edu.mines.gradingadmin.repositories.*;
import edu.mines.gradingadmin.services.external.CanvasService;
import edu.mines.gradingadmin.services.external.PolicyServerService;
import edu.mines.gradingadmin.services.external.S3Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.*;

@Slf4j
@Service
public class CourseService {
    private final CourseRepo courseRepo;
    private final CourseLateRequestConfigRepo lateRequestConfigRepo;
    private final GradescopeConfigRepo gradescopeConfigRepo;
    private final ScheduledTaskRepo<CourseSyncTaskDef> taskRepo;
    private final ApplicationEventPublisher eventPublisher;
    private final ImpersonationManager impersonationManager;
    private final CanvasService canvasService;
    private final S3Service s3Service;
    private final PolicyRepo policyRepo;
    private final UserService userService;
    public CourseService(CourseRepo courseRepo, CourseLateRequestConfigRepo lateRequestConfigRepo, GradescopeConfigRepo gradescopeConfigRepo, ScheduledTaskRepo<CourseSyncTaskDef> taskRepo,

                         ApplicationEventPublisher eventPublisher, ImpersonationManager impersonationManager,
                         CanvasService canvasService, S3Service s3Service, PolicyRepo policyRepo, UserService userService) {

        this.courseRepo = courseRepo;
        this.lateRequestConfigRepo = lateRequestConfigRepo;
        this.gradescopeConfigRepo = gradescopeConfigRepo;
        this.taskRepo = taskRepo;
        this.impersonationManager = impersonationManager;
        this.canvasService = canvasService;
        this.eventPublisher = eventPublisher;
        this.s3Service = s3Service;
        this.policyRepo = policyRepo;
        this.userService = userService;
    }

    public List<Course> getCourses(boolean enabled) {
            if (enabled) {
                return courseRepo.getAll(enabled);
            }
            return courseRepo.getAll();
    }

    public List<Course> getCoursesStudent(User user){
        List<CourseMember> memberships = userService.getCourseMemberships(user.getCwid());
        List<Course> existingCourses = courseRepo.getAll(true);
        List<Course> studentEnrolledCourses = new ArrayList<>();;
        for (CourseMember courseMember : memberships) {
            for(Course course: existingCourses) {
                if (courseMember.getCourse().equals(course)) {
                    studentEnrolledCourses.add(course);
                }
            }
        }
        return studentEnrolledCourses;
    }

    public Optional<Course> getCourse(UUID courseId){
        return courseRepo.getById(courseId);
    }

    public Optional<Course> updateCourse(String courseId, CourseDTO courseDTO) {
        Optional<Course> course = getCourse(UUID.fromString(courseId));

        if(course.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course does not exist");
        }

        course.get().setName(courseDTO.getName());
        course.get().setCode(courseDTO.getCode());
        course.get().setTerm(courseDTO.getTerm());
        course.get().setEnabled(courseDTO.getEnabled());
        CourseLateRequestConfig config = course.get().getLateRequestConfig();
        if (config != null) {
            config.setLatePassesEnabled(courseDTO.getLateRequestConfig().getLatePassesEnabled());
            config.setLatePassName(courseDTO.getLateRequestConfig().getLatePassName());
            config.setEnabledExtensionReasons(courseDTO.getLateRequestConfig().getEnabledExtensionReasons());
            config.setTotalLatePassesAllowed(courseDTO.getLateRequestConfig().getTotalLatePassesAllowed());

            course.get().setLateRequestConfig(lateRequestConfigRepo.save(config));
        }

        GradescopeConfig gsConfig = course.get().getGradescopeConfig();
        if(gsConfig != null) {
            if(courseDTO.getGradescopeId() != null) {
                gsConfig.setGradescopeId(courseDTO.getGradescopeId());
            }
            gsConfig.setEnabled(courseDTO.getEnabled());
            course.get().setGradescopeConfig(gradescopeConfigRepo.save(gsConfig));
        }

        return Optional.of(courseRepo.save(course.get()));
    }

    public void syncCourseTask(CourseSyncTaskDef task){
        IdentityProvider user = impersonationManager.impersonateUser(task.getCreatedByUser());
        List<edu.ksu.canvas.model.Course> availableCourses =
                canvasService.asUser(user).getAllAvailableCourses()
                        .stream()
                        .filter(course -> course.getId() == task.getCanvasId())
                        .toList();

        if (availableCourses.isEmpty()) {
            log.warn("No courses were found with canvas id '{}'", task.getCanvasId());
            throw new RuntimeException(String.format("Failed to find Canvas course: '%d'", task.getCanvasId()));
        }

        if (availableCourses.size() != 1) {
            log.warn("More than one course was found with canvas id '{}'. This shouldn't be possible", task.getCanvasId());
            throw new RuntimeException(String.format("Failed to find Canvas course: '%d'", task.getCanvasId()));
        }

        edu.ksu.canvas.model.Course canvasCourse = availableCourses.getFirst();

        Optional<Course> toUpdate = courseRepo.getById(task.getCourseToSync());

        if (toUpdate.isEmpty()){
            log.warn("Course to sync not found!");
            throw new RuntimeException(String.format("Failed to find course: '%s'", task.getCourseToSync()));
        }

        toUpdate.get().setCanvasId(canvasCourse.getId());
        if (task.shouldOverwriteCode())
            toUpdate.get().setCode(canvasCourse.getCourseCode());
        if (task.shouldOverwriteName())
            toUpdate.get().setName(canvasCourse.getName());

        courseRepo.save(toUpdate.get());
    }

    public Optional<ScheduledTaskDef> syncCourseWithCanvas(User actingUser, UUID courseId, long canvasId,
                                                             boolean overwriteName, boolean overwriteCode) {
        if (!courseRepo.existsById(courseId)){
            log.warn("Course '{}' has not been created!", courseId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course does not exist");
        }

        CourseSyncTaskDef task = new CourseSyncTaskDef();
        task.setTaskName(String.format("Sync Course '%s': Course Data", courseId));
        task.setCreatedByUser(actingUser);
        task.setCourseToSync(courseId);
        task.setCanvasId(canvasId);
        task.shouldOverwriteName(overwriteName);
        task.shouldOverwriteCode(overwriteCode);

        task = taskRepo.save(task);

        NewTaskEvent.TaskData<CourseSyncTaskDef> taskDef = new NewTaskEvent.TaskData<>(taskRepo, task.getId(), this::syncCourseTask);

        eventPublisher.publishEvent(new NewTaskEvent(this, taskDef));

        return Optional.of(task);
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

    public Optional<Course> createNewCourse(CourseDTO courseDTO){
        Course newCourse = new Course();
        newCourse.setName(courseDTO.getName());
        newCourse.setCode(courseDTO.getCode());
        newCourse.setTerm(courseDTO.getTerm());
        newCourse.setEnabled(true);

        Optional<CourseLateRequestConfig> lateRequestConfig = createCourseLateRequestConfig(courseDTO.getLateRequestConfig());

        ExternalServiceConfig externalServiceConfig = new ExternalServiceConfig();
        if(courseDTO.getGradescopeId() != null) {
            ExternalServiceConfig.GradescopeConfig config = externalServiceConfig.configureGradescope(true, URI.create("https://www.gradescope.com/courses/" + courseDTO.getGradescopeId()));

            GradescopeConfig gsConfig = new GradescopeConfig();
            gsConfig.setGradescopeId(courseDTO.getGradescopeId());
            gsConfig.setEnabled(config.isEnabled());
            newCourse.setGradescopeConfig(gradescopeConfigRepo.save(gsConfig));
        }

        if (lateRequestConfig.isPresent()) {
            newCourse.setLateRequestConfig(lateRequestConfig.get());
        }

        newCourse = courseRepo.save(newCourse);

        Optional<String> bucketName = s3Service.createNewBucketForCourse(newCourse.getId());

        if (bucketName.isEmpty()){
            log.warn("Failed to create S3 bucket!");
        }

        return Optional.of(newCourse);
    }

    private Optional<CourseLateRequestConfig> createCourseLateRequestConfig(CourseLateRequestConfigDTO dto) {
        if (dto == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course late request does not exist");
        }
        CourseLateRequestConfig lateRequestConfig = new CourseLateRequestConfig();
        lateRequestConfig.setLatePassesEnabled(dto.getLatePassesEnabled());
        lateRequestConfig.setEnabledExtensionReasons(dto.getEnabledExtensionReasons());
        lateRequestConfig.setTotalLatePassesAllowed(dto.getTotalLatePassesAllowed());
        lateRequestConfig.setLatePassName(dto.getLatePassName());
        return Optional.of(lateRequestConfigRepo.save(lateRequestConfig));
    }


}
