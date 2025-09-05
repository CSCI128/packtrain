package edu.mines.packtrain.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.mines.packtrain.data.CourseDTO;
import edu.mines.packtrain.data.CourseLateRequestConfigDTO;
import edu.mines.packtrain.data.websockets.CourseSyncNotificationDTO;
import edu.mines.packtrain.events.NewTaskEvent;
import edu.mines.packtrain.managers.IdentityProvider;
import edu.mines.packtrain.managers.ImpersonationManager;
import edu.mines.packtrain.models.Course;
import edu.mines.packtrain.models.CourseLateRequestConfig;
import edu.mines.packtrain.models.CourseMember;
import edu.mines.packtrain.models.GradescopeConfig;
import edu.mines.packtrain.models.MasterMigration;
import edu.mines.packtrain.models.User;
import edu.mines.packtrain.models.enums.CourseRole;
import edu.mines.packtrain.models.tasks.CourseSyncTaskDef;
import edu.mines.packtrain.models.tasks.ScheduledTaskDef;
import edu.mines.packtrain.repositories.CourseLateRequestConfigRepo;
import edu.mines.packtrain.repositories.CourseMemberRepo;
import edu.mines.packtrain.repositories.CourseRepo;
import edu.mines.packtrain.repositories.GradescopeConfigRepo;
import edu.mines.packtrain.repositories.MasterMigrationRepo;
import edu.mines.packtrain.repositories.MigrationRepo;
import edu.mines.packtrain.repositories.ScheduledTaskRepo;
import edu.mines.packtrain.services.external.CanvasService;
import edu.mines.packtrain.services.external.S3Service;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
    private final UserService userService;
    private final MasterMigrationRepo masterMigrationRepo;
    private final MigrationRepo migrationRepo;
    private final CourseMemberRepo courseMemberRepo;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CourseService(CourseRepo courseRepo, CourseLateRequestConfigRepo lateRequestConfigRepo,
                         GradescopeConfigRepo gradescopeConfigRepo,
                         ScheduledTaskRepo<CourseSyncTaskDef> taskRepo,
                         ApplicationEventPublisher eventPublisher,
                         ImpersonationManager impersonationManager,
                         CanvasService canvasService, S3Service s3Service, UserService userService,
                         MasterMigrationRepo masterMigrationRepo, MigrationRepo migrationRepo,
                         CourseMemberRepo courseMemberRepo,
                         SimpMessagingTemplate messagingTemplate) {
        this.courseRepo = courseRepo;
        this.lateRequestConfigRepo = lateRequestConfigRepo;
        this.gradescopeConfigRepo = gradescopeConfigRepo;
        this.taskRepo = taskRepo;
        this.impersonationManager = impersonationManager;
        this.canvasService = canvasService;
        this.eventPublisher = eventPublisher;
        this.s3Service = s3Service;
        this.userService = userService;
        this.masterMigrationRepo = masterMigrationRepo;
        this.migrationRepo = migrationRepo;
        this.courseMemberRepo = courseMemberRepo;
        this.messagingTemplate = messagingTemplate;
    }

    public List<Course> getAllCourses(boolean enabled) {
        if (enabled) {
            return courseRepo.getAll(true);
        }
        return courseRepo.getAll();
    }


    public List<Course> getCoursesStudent(User user) {
        return getCoursesByRole(user, CourseRole.STUDENT, true);
    }

    public List<Course> getCoursesByRole(User user, CourseRole courseRole, Boolean enabled) {
        List<CourseMember> memberships = userService.getCourseMemberships(user.getCwid());

        return memberships.stream().filter(m -> m.getRole().equals(courseRole))
                .map(CourseMember::getCourse)
                // only filter on enabled if boolean is non-null - this is why it has the object
                // instead of primitive
                .filter(c -> enabled == null || c.isEnabled() == enabled)
                .toList();
    }

    public Course getCourse(UUID courseId) {
        Optional<Course> course = courseRepo.getById(courseId);
        if (course.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("Course " +
                    "'%s' does not exist", courseId));
        }

        return course.get();
    }

    public Course updateCourse(UUID courseId, CourseDTO courseDTO) {
        Course course = getCourse(courseId);

        course.setName(courseDTO.getName());
        course.setCode(courseDTO.getCode());
        course.setTerm(courseDTO.getTerm());
        course.setEnabled(courseDTO.getEnabled());

        CourseLateRequestConfig config = course.getLateRequestConfig();
        if (config != null) {
            config.setLatePassesEnabled(courseDTO.getLateRequestConfig().getLatePassesEnabled());
            config.setLatePassName(courseDTO.getLateRequestConfig().getLatePassName());
            config.setEnabledExtensionReasons(courseDTO.getLateRequestConfig()
                    .getEnabledExtensionReasons());
            config.setTotalLatePassesAllowed(courseDTO.getLateRequestConfig()
                    .getTotalLatePassesAllowed());

            course.setLateRequestConfig(lateRequestConfigRepo.save(config));
        }

        GradescopeConfig gsConfig = course.getGradescopeConfig();
        if (gsConfig != null) {
            if (courseDTO.getGradescopeId() != null) {
                gsConfig.setGradescopeId(courseDTO.getGradescopeId().toString());
            }
            course.setGradescopeConfig(gradescopeConfigRepo.save(gsConfig));
        }

        return courseRepo.save(course);
    }

    public boolean deleteCourse(UUID courseId) {
        Optional<Course> course = courseRepo.getById(courseId);

        if (course.isEmpty()) {
            log.warn("Attempt to get course that doesn't exist!");
            return false;
        }

        List<MasterMigration> masterMigrations = masterMigrationRepo
                .getMasterMigrationsByCourseId(courseId);

        for (MasterMigration masterMigration : masterMigrations) {
            if (!migrationRepo.getMigrationListByMasterMigrationId(masterMigration.getId())
                    .isEmpty()) {
                log.warn("Attempt to delete course that has existing migrations!");
                return false;
            }
        }

        // must delete accidentally imported course members first
        courseMemberRepo.deleteByCourseAndCwid(course.get(),
                courseMemberRepo.getAllCwidsByCourse(course.get()));

        courseRepo.delete(course.get());

        return true;
    }

    public void syncCourseTask(CourseSyncTaskDef task) {
        IdentityProvider user = impersonationManager.impersonateUser(task.getCreatedByUser());
        List<edu.ksu.canvas.model.Course> availableCourses =
                canvasService.asUser(user).getAllAvailableCourses()
                        .stream()
                        .filter(course -> course.getId() == task.getCanvasId())
                        .toList();

        if (availableCourses.isEmpty()) {
            log.warn("No courses were found with canvas id '{}'", task.getCanvasId());
            throw new RuntimeException(String.format("Failed to find Canvas course: '%d'",
                    task.getCanvasId()));
        }

        if (availableCourses.size() != 1) {
            log.warn("More than one course was found with canvas id '{}'. This shouldn't " +
                    "be possible", task.getCanvasId());
            throw new RuntimeException(String.format("Failed to find Canvas course: '%d'",
                    task.getCanvasId()));
        }

        edu.ksu.canvas.model.Course canvasCourse = availableCourses.getFirst();

        Optional<Course> toUpdate = courseRepo.getById(task.getCourseToSync());

        if (toUpdate.isEmpty()) {
            log.warn("Course to sync not found!");
            throw new RuntimeException(String.format("Failed to find course: '%s'",
                    task.getCourseToSync()));
        }

        toUpdate.get().setCanvasId(canvasCourse.getId());
        if (task.shouldOverwriteCode())
            toUpdate.get().setCode(canvasCourse.getCourseCode());
        if (task.shouldOverwriteName())
            toUpdate.get().setName(canvasCourse.getName());

        courseRepo.save(toUpdate.get());
    }

    public ScheduledTaskDef syncCourseWithCanvas(User actingUser, UUID courseId, long canvasId,
                                                 boolean overwriteName, boolean overwriteCode) {
        if (!courseRepo.existsById(courseId)) {
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

        CourseSyncNotificationDTO notificationDTO = CourseSyncNotificationDTO.builder()
                .courseComplete(true).build();
        NewTaskEvent.TaskData<CourseSyncTaskDef> taskDefinition = new NewTaskEvent.TaskData<>(
                taskRepo, task.getId(), this::syncCourseTask);
        taskDefinition.setOnJobComplete(Optional.of(_ -> {
            try {
                messagingTemplate.convertAndSend("/courses/import",
                        objectMapper.writeValueAsString(notificationDTO));
            } catch (JsonProcessingException _) {
                throw new RuntimeException("Could not process JSON for sending notification DTO!");
            }
        }));

        eventPublisher.publishEvent(new NewTaskEvent(this, taskDefinition));

        return task;
    }

    public void enableCourse(UUID courseId) {
        Optional<Course> course = courseRepo.findById(courseId);
        if (course.isPresent()) {
            course.get().setEnabled(true);
            courseRepo.save(course.get());
        }
    }

    public void disableCourse(UUID courseId) {
        Optional<Course> course = courseRepo.findById(courseId);
        if (course.isPresent()) {
            course.get().setEnabled(false);
            courseRepo.save(course.get());
        }
    }

    public Course createNewCourse(CourseDTO courseDTO) {
        Course newCourse = new Course();
        newCourse.setName(courseDTO.getName());
        newCourse.setCode(courseDTO.getCode());
        newCourse.setTerm(courseDTO.getTerm());
        newCourse.setEnabled(true);

        Optional<CourseLateRequestConfig> lateRequestConfig =
                createCourseLateRequestConfig(courseDTO.getLateRequestConfig());

        if (courseDTO.getGradescopeId() != null) {
            GradescopeConfig gsConfig = new GradescopeConfig();
            gsConfig.setGradescopeId(courseDTO.getGradescopeId().toString());
            newCourse.setGradescopeConfig(gradescopeConfigRepo.save(gsConfig));
        }

        if (lateRequestConfig.isPresent()) {
            newCourse.setLateRequestConfig(lateRequestConfig.get());
        }

        newCourse = courseRepo.save(newCourse);

        Optional<String> bucketName = s3Service.createNewBucketForCourse(newCourse.getId());


        if (bucketName.isEmpty()) {
            log.error("Failed to create S3 bucket for course!");
            log.error("Cleaning up course");
            courseRepo.delete(newCourse);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("Failed to " +
                    "create S3 bucket for course '%s'!", courseDTO.getCode()));
        }


        return newCourse;
    }

    private Optional<CourseLateRequestConfig> createCourseLateRequestConfig(
            CourseLateRequestConfigDTO dto) {
        if (dto == null) {
            return Optional.empty();
        }
        CourseLateRequestConfig lateRequestConfig = new CourseLateRequestConfig();
        lateRequestConfig.setLatePassesEnabled(dto.getLatePassesEnabled());
        lateRequestConfig.setEnabledExtensionReasons(dto.getEnabledExtensionReasons());
        lateRequestConfig.setTotalLatePassesAllowed(dto.getTotalLatePassesAllowed());
        lateRequestConfig.setLatePassName(dto.getLatePassName());
        return Optional.of(lateRequestConfigRepo.save(lateRequestConfig));
    }


    public Course getCourseForMigration(UUID migrationId) {
        Optional<Course> course = courseRepo.getCourseByMigrationId(migrationId);

        if (course.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("No course " +
                    "exists for migration '%s'!", migrationId));
        }

        return course.get();
    }

    public Course getCourseForMasterMigration(UUID masterMigrationId) {
        Optional<Course> course = courseRepo.getCourseByMasterMigrationId(masterMigrationId);

        if (course.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("No course " +
                    "exists for master migration '%s'!", masterMigrationId));
        }

        return course.get();

    }
}
