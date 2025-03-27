package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.data.CourseDTO;
import edu.mines.gradingadmin.data.CourseLateRequestConfigDTO;
import edu.mines.gradingadmin.events.NewTaskEvent;
import edu.mines.gradingadmin.managers.IdentityProvider;
import edu.mines.gradingadmin.managers.ImpersonationManager;
import edu.mines.gradingadmin.models.*;
import edu.mines.gradingadmin.models.tasks.ScheduledTaskDef;
import edu.mines.gradingadmin.models.tasks.CourseSyncTaskDef;
import edu.mines.gradingadmin.repositories.CourseLateRequestConfigRepo;
import edu.mines.gradingadmin.repositories.CourseRepo;
import edu.mines.gradingadmin.repositories.PolicyRepo;
import edu.mines.gradingadmin.repositories.ScheduledTaskRepo;
import edu.mines.gradingadmin.services.external.CanvasService;
import edu.mines.gradingadmin.services.external.S3Service;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.*;

@Slf4j
@Service
public class CourseService {
    private final CourseRepo courseRepo;
    private final CourseLateRequestConfigRepo lateRequestConfigRepo;
    private final ScheduledTaskRepo<CourseSyncTaskDef> taskRepo;

    private final ApplicationEventPublisher eventPublisher;
    private final ImpersonationManager impersonationManager;
    private final CanvasService canvasService;
    private final S3Service s3Service;
    private final PolicyRepo policyRepo;

    public CourseService(CourseRepo courseRepo, CourseLateRequestConfigRepo lateRequestConfigRepo, ScheduledTaskRepo<CourseSyncTaskDef> taskRepo,
                         ApplicationEventPublisher eventPublisher, ImpersonationManager impersonationManager,
                         CanvasService canvasService, S3Service s3Service, PolicyRepo policyRepo) {
        this.courseRepo = courseRepo;
        this.lateRequestConfigRepo = lateRequestConfigRepo;
        this.taskRepo = taskRepo;
        this.impersonationManager = impersonationManager;
        this.canvasService = canvasService;
        this.eventPublisher = eventPublisher;
        this.s3Service = s3Service;
        this.policyRepo = policyRepo;
    }

    public List<Course> getCourses(boolean enabled) {
        if (enabled) {
            return courseRepo.getAll(enabled);
        }
        return courseRepo.getAll();
    }

    public Optional<Course> getCourse(UUID courseId){
        return courseRepo.getById(courseId);
    }

    public Optional<Course> updateCourse(String courseId, CourseDTO courseDTO) {
        Optional<Course> course = getCourse(UUID.fromString(courseId));

        if(course.isEmpty()) {
            return Optional.empty();
        }

        course.get().setName(courseDTO.getName());
        course.get().setCode(courseDTO.getCode());
        course.get().setTerm(courseDTO.getTerm());
        course.get().setEnabled(courseDTO.getEnabled());
        CourseLateRequestConfig config = course.get().getLateRequestConfig();
        config.setLatePassesEnabled(courseDTO.getLateRequestConfig().getLatePassesEnabled());
        config.setLatePassName(courseDTO.getLateRequestConfig().getLatePassName());
        config.setEnabledExtensionReasons(courseDTO.getLateRequestConfig().getEnabledExtensionReasons());
        config.setTotalLatePassesAllowed(courseDTO.getLateRequestConfig().getTotalLatePassesAllowed());

        course.get().setLateRequestConfig(lateRequestConfigRepo.save(config));

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
            return Optional.empty();
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

        CourseLateRequestConfig lateRequestConfig = createCourseLateRequestConfig(courseDTO.getLateRequestConfig());

        newCourse.setLateRequestConfig(lateRequestConfig);

        newCourse = courseRepo.save(newCourse);

        Optional<String> bucketName = s3Service.createNewBucketForCourse(newCourse.getId());

        if (bucketName.isEmpty()){
            log.warn("Failed to create S3 bucket!");
        }

        return Optional.of(newCourse);
    }

    private CourseLateRequestConfig createCourseLateRequestConfig(CourseLateRequestConfigDTO dto) {
        CourseLateRequestConfig lateRequestConfig = new CourseLateRequestConfig();
        lateRequestConfig.setLatePassesEnabled(dto.getLatePassesEnabled());
        lateRequestConfig.setEnabledExtensionReasons(dto.getEnabledExtensionReasons());
        lateRequestConfig.setTotalLatePassesAllowed(dto.getTotalLatePassesAllowed());
        lateRequestConfig.setLatePassName(dto.getLatePassName());
        return lateRequestConfigRepo.save(lateRequestConfig);
    }

    public Optional<Policy> createNewCourseWidePolicy(User actingUser, UUID courseId, String policyName, String fileName, MultipartFile file){
        // if this is slow, we may need to make this a task
        Optional<Course> course = courseRepo.findById(courseId);

        if (course.isEmpty()){
            log.warn("Course '{}' does not exist!", courseId);
            return Optional.empty();
        }

        log.debug("Creating new course wide policy '{}' for course '{}'", policyName, course.get().getCode());

        Optional<String> policyUrl = s3Service.uploadCourseWidePolicy(actingUser, courseId, fileName, file);

        if (policyUrl.isEmpty()){
            log.warn("Failed to upload policy '{}'", policyName);
            return Optional.empty();
        }

        // this should never happen, but if it does, then we also need to reject it as the URIs must be unique
        if (policyRepo.existsByPolicyURI(policyUrl.get())){
            log.warn("Policy already exists at url '{}'", policyUrl.get());
            return Optional.empty();
        }

        Policy policy = new Policy();
        policy.setCourse(course.get());
        policy.setCreatedByUser(actingUser);
        policy.setPolicyName(policyName);
        policy.setPolicyURI(policyUrl.get());

        policy = policyRepo.save(policy);

        log.info("Created new policy '{}' for course '{}' at '{}'", policyName, course.get().getCode(), policyUrl.get());

        return Optional.of(policy);
    }

    public List<Policy> getAllPolicies(UUID courseId){
        Optional<Course> course = courseRepo.findById(courseId);

        if (course.isEmpty()){
            return List.of();
        }

        return policyRepo.getPoliciesByCourse(course.get());
    }

    public Optional<Policy> getPolicy(URI policyURI){
        Optional<Policy> policy = policyRepo.getPolicyByURI(policyURI.toString());

        if (policy.isEmpty()){
            return Optional.empty();
        }

        return policy;
    }

}
